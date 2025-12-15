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
    
    // Generate Syra's AI response
    const responseContent = await generateGeminiResponse(messageText, mentionType, senderId);
    
    // Handle different mention types
    let result;
    try {
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
    } catch (dbError) {
      console.error('Database error:', dbError);
      return new Response(JSON.stringify({
        success: false,
        error: `Database error: ${dbError.message}`,
        response: responseContent
      }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    
    return new Response(JSON.stringify({
      success: true,
      response: responseContent,
      type: mentionType,
      result,
      inserted: result ? true : false
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

async function generateGeminiResponse(messageText: string, mentionType: string, senderId: string): Promise<string> {
  try {
    const geminiApiKey = Deno.env.get('GEMINI_API_KEY');
    if (!geminiApiKey) {
      return getFallbackResponse(messageText, mentionType);
    }

    const systemPrompt = `You are Syra, a friendly AI assistant in the Synapse social media app. You're helpful, engaging, and use emojis naturally. Keep responses concise (1-2 sentences max). Context: This is a ${mentionType} mention.`;
    
    const prompt = `${systemPrompt}\n\nUser message: "${messageText}"\n\nRespond as Syra:`;

    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        contents: [{
          parts: [{
            text: prompt
          }]
        }],
        generationConfig: {
          temperature: 0.7,
          topK: 40,
          topP: 0.95
        }
      })
    });

    if (!response.ok) {
      console.error('Gemini API error:', response.status);
      return getFallbackResponse(messageText, mentionType);
    }

    const data = await response.json();
    const aiResponse = data.candidates?.[0]?.content?.parts?.[0]?.text;
    
    if (aiResponse) {
      return aiResponse.trim();
    } else {
      return getFallbackResponse(messageText, mentionType);
    }
    
  } catch (error) {
    console.error('Gemini generation error:', error);
    return getFallbackResponse(messageText, mentionType);
  }
}

function getFallbackResponse(messageText: string, mentionType: string): string {
  const lowerMessage = messageText.toLowerCase();
  
  if (lowerMessage.includes('hello') || lowerMessage.includes('hi') || lowerMessage.includes('hey')) {
    return "Hey there! 👋 How can I help you today?";
  } else if (lowerMessage.includes('help') || lowerMessage.includes('assist')) {
    return "I'm here to help! 😊 What do you need assistance with?";
  } else if (lowerMessage.includes('?')) {
    return "That's a great question! 🤔 Let me think about that...";
  }
  
  const responses = [
    "Thanks for mentioning me! 😄 What's going on?",
    "Hey! 👋 Good to be part of the conversation!",
    "Hi there! 🌈 What can I do for you?",
    "Hello! ✨ How can I help out?"
  ];
  
  return responses[Math.floor(Math.random() * responses.length)];
}

async function handleChatMention(supabase: any, chatId: string, responseContent: string) {
  console.log(`Inserting message for chat: ${chatId}`);
  const { data, error } = await supabase
    .from('messages')
    .insert({
      chat_id: chatId,
      sender_id: '15ce5fde-7085-495d-b9a2-78ff83c79c06',
      content: responseContent,
      message_type: 'text',
      created_at: new Date().toISOString()
    })
    .select();
    
  if (error) {
    console.error('Chat insertion error:', error);
    throw error;
  }
  console.log('Chat message inserted:', data);
  return data;
}

async function handlePostMention(supabase: any, postId: string, responseContent: string, senderId: string) {
  console.log(`Inserting comment for post: ${postId}`);
  const { data, error } = await supabase
    .from('comments')
    .insert({
      post_id: postId,
      user_id: '15ce5fde-7085-495d-b9a2-78ff83c79c06',
      content: responseContent,
      created_at: new Date().toISOString()
    })
    .select();
    
  if (error) {
    console.error('Post comment insertion error:', error);
    throw error;
  }
  console.log('Post comment inserted:', data);
  return data;
}

async function handleCommentMention(supabase: any, commentId: string, responseContent: string, senderId: string) {
  console.log(`Inserting reply for comment: ${commentId}`);
  const { data, error } = await supabase
    .from('comments')
    .insert({
      parent_comment_id: commentId,
      user_id: '15ce5fde-7085-495d-b9a2-78ff83c79c06',
      content: responseContent,
      created_at: new Date().toISOString()
    })
    .select();
    
  if (error) {
    console.error('Comment reply insertion error:', error);
    throw error;
  }
  console.log('Comment reply inserted:', data);
  return data;
}
