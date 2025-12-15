import "jsr:@supabase/functions-js/edge-runtime.d.ts";

Deno.serve(async (req: Request) => {
  try {
    const { content, chatId, postId, commentId, senderId, type } = await req.json();
    
    // Check if @syra is mentioned
    if (!content.toLowerCase().includes('@syra')) {
      return new Response(JSON.stringify({ mentioned: false }));
    }
    
    // Call the existing mention handler
    const handlerResponse = await fetch(`${Deno.env.get('SUPABASE_URL')}/functions/v1/syra-mention-handler`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        chatId,
        postId, 
        commentId,
        messageText: content,
        mentionedUsers: ['syra'],
        senderId,
        mentionType: type
      })
    });
    
    const result = await handlerResponse.json();
    
    return new Response(JSON.stringify({
      mentioned: true,
      syraResponse: result
    }));
    
  } catch (error) {
    return new Response(JSON.stringify({
      error: error.message
    }), { status: 400 });
  }
});
