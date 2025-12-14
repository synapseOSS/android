import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface InferenceRequest {
  user_id: string;
  prompt: string;
  model?: string;
  use_fine_tuned?: boolean;
  context?: string[];
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { user_id, prompt, model = 'gpt-3.5-turbo', use_fine_tuned = true, context = [] }: InferenceRequest = await req.json();

    // Get user preferences and fine-tuned model
    const userModel = use_fine_tuned ? await getUserFineTunedModel(user_id) : null;
    const preferences = await getUserPreferences(user_id);

    // Build personalized prompt
    const personalizedPrompt = await buildPersonalizedPrompt(prompt, preferences, context);

    // Make AI inference
    const response = await makeInference(personalizedPrompt, userModel?.model_id || model, preferences);

    // Store interaction for future fine-tuning
    await storeInteraction(user_id, prompt, response, userModel?.job_id);

    return new Response(JSON.stringify({
      success: true,
      response: response.content,
      model_used: userModel?.model_id || model,
      personalized: !!userModel,
      tokens_used: response.tokens,
      cost: response.cost
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

async function getUserFineTunedModel(user_id: string) {
  const { data, error } = await supabase
    .from('fine_tuning_jobs')
    .select('*')
    .eq('user_id', user_id)
    .eq('status', 'deployed')
    .order('created_at', { ascending: false })
    .limit(1)
    .single();

  if (error) return null;
  return data;
}

async function getUserPreferences(user_id: string) {
  const { data, error } = await supabase
    .from('user_ai_preferences')
    .select('preferences')
    .eq('user_id', user_id)
    .single();

  if (error) {
    return {
      response_style: 'balanced',
      tone: 'friendly',
      length: 'medium',
      technical_level: 'intermediate'
    };
  }

  return data.preferences;
}

async function buildPersonalizedPrompt(prompt: string, preferences: any, context: string[]) {
  let systemPrompt = `You are a helpful AI assistant. Respond in a ${preferences.tone} tone with ${preferences.length} responses at a ${preferences.technical_level} technical level.`;

  if (preferences.response_style === 'detailed') {
    systemPrompt += ' Provide comprehensive explanations with examples.';
  } else if (preferences.response_style === 'concise') {
    systemPrompt += ' Be direct and to the point.';
  }

  let fullPrompt = systemPrompt + '\n\n';

  if (context.length > 0) {
    fullPrompt += 'Previous context:\n' + context.join('\n') + '\n\n';
  }

  fullPrompt += `User: ${prompt}`;

  return fullPrompt;
}

async function makeInference(prompt: string, model: string, preferences: any) {
  // Simulate AI inference (replace with actual API calls)
  const mockResponses = {
    friendly: "I'd be happy to help you with that! ",
    formal: "I shall assist you with your inquiry. ",
    casual: "Sure thing! "
  };

  const baseResponse = mockResponses[preferences.tone as keyof typeof mockResponses] || mockResponses.friendly;
  
  // Simulate response based on length preference
  let content = baseResponse;
  if (preferences.length === 'short') {
    content += "Here's a quick answer to your question.";
  } else if (preferences.length === 'long') {
    content += "Let me provide you with a comprehensive explanation that covers all the important aspects of your question, including relevant details and examples.";
  } else {
    content += "Here's what you need to know about your question.";
  }

  return {
    content,
    tokens: Math.floor(content.length / 4),
    cost: Math.floor(content.length / 4) * 0.0001
  };
}

async function storeInteraction(user_id: string, prompt: string, response: any, job_id?: string) {
  const { error } = await supabase
    .from('ai_interactions')
    .insert({
      user_id,
      prompt,
      response: response.content,
      tokens_used: response.tokens,
      cost: response.cost,
      job_id,
      created_at: new Date().toISOString()
    });

  if (error) console.error('Failed to store interaction:', error);
}
