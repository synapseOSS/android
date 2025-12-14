import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

interface MentionRequest {
  chatId?: string;
  postId?: string;
  commentId?: string;
  messageText: string;
  mentionedUsers: string[];
  senderId: string;
  mentionType: 'chat' | 'post' | 'comment';
}

Deno.serve(async (req: Request) => {
  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseKey);
    
    const mentionData: MentionRequest = await req.json();
    const { chatId, postId, commentId, messageText, mentionedUsers, senderId, mentionType } = mentionData;
    
    // Check if Syra is mentioned
    if (!mentionedUsers.includes('syra')) {
      return new Response(JSON.stringify({ 
        success: false, 
        message: 'Syra not mentioned' 
      }), {
        headers: { 'Content-Type': 'application/json' }
      });
    }
    
    // Generate Syra's response
    const responseContent = await generateSyraResponse(messageText, mentionType, senderId);
    
    // Handle different mention types
    let result;
    switch (mentionType) {
      case 'chat':
        result = await handleChatMention(supabase, chatId!, responseContent);
        break;
      case 'post':
        result = await handlePostMention(supabase, postId!, responseContent, senderId);
        break;
      case 'comment':
        result = await handleCommentMention(supabase, commentId!, responseContent, senderId);
        break;
      default:
        throw new Error('Invalid mention type');
    }
    
    return new Response(JSON.stringify({
      success: true,
      response: responseContent,
      type: mentionType,
      result
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
    
  } catch (error) {
    return new Response(JSON.stringify({
      success: false,
      error: error.message
    }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});

async function generateSyraResponse(messageText: string, mentionType: string, senderId: string): Promise<string> {
  const lowerMessage = messageText.toLowerCase();
  
  // Context-aware responses based on mention type
  if (mentionType === 'chat') {
    if (lowerMessage.includes('help') || lowerMessage.includes('assist')) {
      return "I'm here to help! 😊 What can I assist you with in this chat?";
    }
    return "Hey! Thanks for adding me to the conversation! 💫 How can I help you all?";
  }
  
  if (mentionType === 'post') {
    if (lowerMessage.includes('opinion') || lowerMessage.includes('think')) {
      return "Great question! 🤔 I think this is really interesting. What's everyone else's take on this?";
    }
    return "Thanks for the mention! 🌟 This looks awesome! Keep up the great content! ✨";
  }
  
  if (mentionType === 'comment') {
    return "Thanks for including me in the discussion! 💬 I love seeing the community engage like this! 🎉";
  }
  
  return "Hey there! 👋 Thanks for mentioning me! How can I help? 💫";
}

async function handleChatMention(supabase: any, chatId: string, responseContent: string) {
  // Insert Syra's message in the chat
  const { data, error } = await supabase
    .from('messages')
    .insert({
      chat_id: chatId,
      sender_id: 'syra-ai-uid',
      content: responseContent,
      message_type: 'text',
      created_at: new Date().toISOString()
    });
    
  if (error) throw error;
  return data;
}

async function handlePostMention(supabase: any, postId: string, responseContent: string, senderId: string) {
  // Add Syra's comment to the post
  const { data, error } = await supabase
    .from('comments')
    .insert({
      post_id: postId,
      user_id: 'syra-ai-uid',
      content: responseContent,
      created_at: new Date().toISOString()
    });
    
  if (error) throw error;
  return data;
}

async function handleCommentMention(supabase: any, commentId: string, responseContent: string, senderId: string) {
  // Reply to the comment
  const { data, error } = await supabase
    .from('comments')
    .insert({
      parent_comment_id: commentId,
      user_id: 'syra-ai-uid',
      content: responseContent,
      created_at: new Date().toISOString()
    });
    
  if (error) throw error;
  return data;
}
