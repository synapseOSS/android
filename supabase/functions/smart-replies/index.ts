import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface SmartReplyRequest {
  user_id: string;
  message: string;
  chat_context?: string[];
}

function generateSmartReplies(message: string): string[] {
  const lower = message.toLowerCase();
  
  if (lower.includes('how are you') || lower.includes('how\'s it going')) {
    return ["I'm doing great, thanks!", "Pretty good! How about you?", "All good here! 😊"];
  }
  
  if (lower.includes('thank') || lower.includes('thanks')) {
    return ["You're welcome!", "No problem!", "Happy to help! 😊"];
  }
  
  if (lower.includes('?')) {
    return ["Let me think about that", "Good question!", "I'm not sure, what do you think?"];
  }
  
  if (lower.includes('meeting') || lower.includes('call')) {
    return ["Sounds good!", "What time works?", "I'll be there"];
  }
  
  return ["That's interesting!", "Tell me more", "I agree", "Makes sense"];
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { user_id, message, chat_context }: SmartReplyRequest = await req.json();
    
    // Generate smart reply suggestions
    const suggestions = generateSmartReplies(message);
    
    // Save context for learning
    await supabase.from('ai_chat_context').insert({
      session_id: null, // Global context
      context_type: 'smart_reply_usage',
      context_content: {
        original_message: message,
        suggestions,
        user_id,
        timestamp: new Date().toISOString()
      }
    });
    
    return new Response(JSON.stringify({
      suggestions,
      message_analyzed: message,
      context_saved: true
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
