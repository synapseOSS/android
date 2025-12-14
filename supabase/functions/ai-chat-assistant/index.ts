import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface ChatRequest {
  user_id: string;
  message: string;
  chat_id?: string;
  session_type?: 'assistant' | 'smart_reply' | 'context_help';
}

async function generateResponse(message: string, context: any): Promise<string> {
  const responses = {
    greeting: ["Hello! How can I help you today?", "Hi there! What can I assist you with?"],
    help: ["I'm here to help! You can ask me about Synapse features, get suggestions, or just chat.", "Feel free to ask me anything about using Synapse or general questions!"],
    features: ["Synapse has posts, stories, messaging, voice/video calls, and AI assistance!", "You can create posts, share stories, chat with friends, and discover new content!"],
    default: ["That's interesting! Tell me more.", "I understand. How can I help with that?", "Thanks for sharing! What would you like to do next?"]
  };
  
  const lower = message.toLowerCase();
  if (lower.includes('hello') || lower.includes('hi')) return responses.greeting[0];
  if (lower.includes('help') || lower.includes('what')) return responses.help[0];
  if (lower.includes('feature') || lower.includes('can do')) return responses.features[0];
  
  return responses.default[Math.floor(Math.random() * responses.default.length)];
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { user_id, message, chat_id, session_type = 'assistant' }: ChatRequest = await req.json();
    const startTime = Date.now();
    
    // Get or create session
    let { data: session } = await supabase
      .from('ai_chat_sessions')
      .select('*')
      .eq('user_id', user_id)
      .eq('is_active', true)
      .single();
    
    if (!session) {
      const { data: newSession } = await supabase
        .from('ai_chat_sessions')
        .insert({ user_id, chat_id, session_type })
        .select()
        .single();
      session = newSession;
    }
    
    // Get recent context
    const { data: context } = await supabase
      .from('ai_chat_context')
      .select('*')
      .eq('session_id', session.id)
      .order('created_at', { ascending: false })
      .limit(5);
    
    // Generate AI response
    const aiResponse = await generateResponse(message, context);
    const responseTime = Date.now() - startTime;
    
    // Save response
    await supabase.from('ai_chat_responses').insert({
      session_id: session.id,
      user_message: message,
      ai_response: aiResponse,
      response_type: session_type,
      response_time_ms: responseTime,
      tokens_used: Math.ceil(message.length / 4) + Math.ceil(aiResponse.length / 4)
    });
    
    // Update session
    await supabase.from('ai_chat_sessions')
      .update({ updated_at: new Date().toISOString() })
      .eq('id', session.id);
    
    return new Response(JSON.stringify({
      response: aiResponse,
      session_id: session.id,
      response_time_ms: responseTime,
      type: session_type
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
    
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});
