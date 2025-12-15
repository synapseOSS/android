import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

Deno.serve(async (req: Request) => {
  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseKey);
    
    const { record, type } = await req.json();
    
    // Check for @syra mentions in content
    const content = record.content || record.message_text || '';
    if (!content.toLowerCase().includes('@syra')) {
      return new Response(JSON.stringify({ success: false, message: 'No Syra mention found' }));
    }
    
    // Determine mention type and trigger handler
    let mentionType: string;
    let contextId: string;
    
    if (record.chat_id) {
      mentionType = 'chat';
      contextId = record.chat_id;
    } else if (record.post_id) {
      mentionType = 'comment';
      contextId = record.id;
    } else {
      mentionType = 'post';
      contextId = record.id;
    }
    
    // Call mention handler
    const handlerResponse = await fetch(`${supabaseUrl}/functions/v1/syra-mention-handler`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${supabaseKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        chatId: mentionType === 'chat' ? contextId : undefined,
        postId: mentionType === 'post' ? contextId : undefined,
        commentId: mentionType === 'comment' ? contextId : undefined,
        messageText: content,
        mentionedUsers: ['syra'],
        senderId: record.user_id || record.sender_id,
        mentionType
      })
    });
    
    const result = await handlerResponse.json();
    
    return new Response(JSON.stringify({
      success: true,
      triggered: true,
      mentionType,
      result
    }));
    
  } catch (error) {
    return new Response(JSON.stringify({
      success: false,
      error: error.message
    }), { status: 400 });
  }
});
