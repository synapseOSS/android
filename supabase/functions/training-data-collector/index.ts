import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface TrainingDataRequest {
  action: 'collect' | 'rate' | 'export' | 'auto_collect';
  user_id: string;
  interaction_id?: string;
  rating?: number;
  feedback?: string;
  auto_collect_days?: number;
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { action, user_id, interaction_id, rating, feedback, auto_collect_days = 30 }: TrainingDataRequest = await req.json();

    let result;
    switch (action) {
      case 'collect':
        result = await collectTrainingData(user_id, auto_collect_days);
        break;
      case 'rate':
        result = await rateInteraction(interaction_id!, rating!, feedback);
        break;
      case 'export':
        result = await exportTrainingData(user_id);
        break;
      case 'auto_collect':
        result = await autoCollectFromConversations(user_id);
        break;
      default:
        throw new Error('Invalid action');
    }

    return new Response(JSON.stringify({
      success: true,
      action,
      data: result
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({
      success: false,
      error: error.message
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});

async function collectTrainingData(user_id: string, days: number) {
  const startDate = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString();

  // Get rated interactions
  const { data: interactions, error } = await supabase
    .from('ai_interactions')
    .select('*')
    .eq('user_id', user_id)
    .gte('created_at', startDate)
    .not('rating', 'is', null)
    .order('created_at', { ascending: false });

  if (error) throw error;

  // Filter high-quality interactions (rating >= 4)
  const qualityData = interactions?.filter(item => item.rating >= 4) || [];

  // Get conversation context for each interaction
  const trainingExamples = await Promise.all(
    qualityData.map(async (interaction) => {
      const context = await getConversationContext(user_id, interaction.created_at);
      return {
        prompt: interaction.prompt,
        response: interaction.response,
        rating: interaction.rating,
        context: context.slice(0, 3), // Last 3 messages for context
        feedback: interaction.feedback,
        created_at: interaction.created_at
      };
    })
  );

  return {
    total_interactions: interactions?.length || 0,
    quality_examples: trainingExamples.length,
    training_data: trainingExamples,
    ready_for_training: trainingExamples.length >= 10
  };
}

async function rateInteraction(interaction_id: string, rating: number, feedback?: string) {
  const { error } = await supabase
    .from('ai_interactions')
    .update({
      rating,
      feedback,
      rated_at: new Date().toISOString()
    })
    .eq('id', interaction_id);

  if (error) throw error;

  return {
    interaction_id,
    rating,
    feedback,
    message: 'Interaction rated successfully'
  };
}

async function exportTrainingData(user_id: string) {
  const { data, error } = await supabase
    .from('ai_interactions')
    .select('prompt, response, rating, feedback, created_at')
    .eq('user_id', user_id)
    .not('rating', 'is', null)
    .gte('rating', 4)
    .order('created_at', { ascending: false });

  if (error) throw error;

  // Format for fine-tuning
  const formatted = data?.map(item => ({
    messages: [
      { role: 'user', content: item.prompt },
      { role: 'assistant', content: item.response }
    ],
    rating: item.rating,
    feedback: item.feedback
  })) || [];

  return {
    total_examples: formatted.length,
    format: 'openai_fine_tuning',
    data: formatted,
    export_date: new Date().toISOString()
  };
}

async function autoCollectFromConversations(user_id: string) {
  // Get recent conversations
  const { data: conversations, error } = await supabase
    .from('conversations')
    .select('id, messages')
    .eq('user_id', user_id)
    .gte('created_at', new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString())
    .order('created_at', { ascending: false })
    .limit(50);

  if (error) throw error;

  const trainingPairs = [];

  // Extract Q&A pairs from conversations
  for (const conv of conversations || []) {
    const messages = conv.messages || [];
    for (let i = 0; i < messages.length - 1; i++) {
      const userMsg = messages[i];
      const aiMsg = messages[i + 1];

      if (userMsg.sender_type === 'user' && aiMsg.sender_type === 'ai') {
        // Auto-rate based on message length and engagement
        const rating = autoRateInteraction(userMsg.content, aiMsg.content);
        
        if (rating >= 4) {
          trainingPairs.push({
            prompt: userMsg.content,
            response: aiMsg.content,
            rating,
            source: 'auto_collected',
            conversation_id: conv.id,
            created_at: aiMsg.created_at
          });
        }
      }
    }
  }

  // Store auto-collected training data
  if (trainingPairs.length > 0) {
    const { error: insertError } = await supabase
      .from('ai_interactions')
      .insert(trainingPairs.map(pair => ({
        user_id,
        prompt: pair.prompt,
        response: pair.response,
        rating: pair.rating,
        feedback: 'Auto-collected from conversation',
        conversation_id: pair.conversation_id,
        created_at: pair.created_at
      })));

    if (insertError) throw insertError;
  }

  return {
    conversations_analyzed: conversations?.length || 0,
    training_pairs_found: trainingPairs.length,
    auto_collected: trainingPairs.length,
    message: `Auto-collected ${trainingPairs.length} training examples`
  };
}

async function getConversationContext(user_id: string, timestamp: string) {
  const { data, error } = await supabase
    .from('ai_interactions')
    .select('prompt, response')
    .eq('user_id', user_id)
    .lt('created_at', timestamp)
    .order('created_at', { ascending: false })
    .limit(5);

  if (error) return [];

  return data?.map(item => `User: ${item.prompt}\nAI: ${item.response}`) || [];
}

function autoRateInteraction(prompt: string, response: string): number {
  let score = 3; // Base score

  // Length indicators
  if (response.length > 100 && response.length < 1000) score += 0.5;
  if (prompt.length > 20) score += 0.5;

  // Quality indicators
  if (response.includes('?')) score += 0.3; // Asks clarifying questions
  if (response.match(/\b(example|for instance|such as)\b/i)) score += 0.4; // Provides examples
  if (response.split('.').length > 2) score += 0.3; // Multiple sentences

  // Engagement indicators
  if (response.match(/\b(help|assist|support)\b/i)) score += 0.2;
  if (response.match(/\b(please|thank|welcome)\b/i)) score += 0.3;

  return Math.min(5, Math.max(1, Math.round(score * 2) / 2)); // Round to nearest 0.5
}
