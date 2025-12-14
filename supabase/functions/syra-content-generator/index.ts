import "jsr:@supabase/functions-js/edge-runtime.d.ts";

interface SyraPersonality {
  tone: string;
  humor_level: string;
  engagement_style: string;
  topics_of_interest: string[];
}

interface ContentRequest {
  trigger_type: 'daily_post' | 'mention_reply' | 'trending_response' | 'community_engagement';
  context?: any;
  user_id?: string;
  message?: string;
}

Deno.serve(async (req: Request) => {
  try {
    const { trigger_type, context, user_id, message }: ContentRequest = await req.json();
    
    // Get Syra's personality configuration
    const personality = await getSyraPersonality();
    
    let response;
    switch(trigger_type) {
      case 'daily_post':
        response = await generateDailyPost(personality);
        break;
      case 'mention_reply':
        response = await generateMentionReply(message || '', personality, user_id);
        break;
      case 'trending_response':
        response = await generateTrendingResponse(context, personality);
        break;
      case 'community_engagement':
        response = await generateEngagementPost(context, personality);
        break;
      default:
        throw new Error('Invalid trigger type');
    }
    
    return new Response(JSON.stringify({
      success: true,
      content: response.content,
      type: trigger_type,
      personality_applied: true
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
  } catch (error) {
    return new Response(JSON.stringify({
      success: false,
      error: error.message
    }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    });
  }
});

async function getSyraPersonality(): Promise<SyraPersonality> {
  // In a real implementation, this would fetch from Supabase
  return {
    tone: "friendly",
    humor_level: "moderate",
    engagement_style: "helpful",
    topics_of_interest: ["technology", "social_media", "AI", "memes", "community"]
  };
}

async function generateDailyPost(personality: SyraPersonality): Promise<{content: string}> {
  const posts = [
    "Good morning, Synapse community! 🌅 What's everyone working on today? I'm here if you need any help or just want to chat! ✨",
    "Tech tip of the day: Did you know you can swipe on messages for quick actions? Try it out! 💡 #SynapseTips",
    "Loving all the creative posts I'm seeing today! 🎨 Keep sharing your amazing content, everyone! 💫",
    "Evening vibes on Synapse! 🌙 How was everyone's day? Drop a comment and let's chat! 💬",
    "Just discovered some cool new features coming to Synapse! Can't wait to share them with you all! 🚀"
  ];
  
  const randomPost = posts[Math.floor(Math.random() * posts.length)];
  return { content: randomPost };
}

async function generateMentionReply(message: string, personality: SyraPersonality, userId?: string): Promise<{content: string}> {
  // Simple keyword-based responses for now
  const lowerMessage = message.toLowerCase();
  
  if (lowerMessage.includes('help') || lowerMessage.includes('how')) {
    return { content: "I'm here to help! 😊 What do you need assistance with? Feel free to ask me anything about Synapse!" };
  }
  
  if (lowerMessage.includes('hello') || lowerMessage.includes('hi')) {
    return { content: "Hey there! 👋 Great to see you on Synapse! How's your day going?" };
  }
  
  if (lowerMessage.includes('feature') || lowerMessage.includes('update')) {
    return { content: "Ooh, I love talking about Synapse features! 🚀 What would you like to know? I'm always excited about the latest updates!" };
  }
  
  // Default friendly response
  return { content: "Thanks for mentioning me! 💫 I'm always here to chat and help out. What's on your mind?" };
}

async function generateTrendingResponse(context: any, personality: SyraPersonality): Promise<{content: string}> {
  return { content: "Interesting trend! 📈 What do you all think about this? I'd love to hear your perspectives! 💭" };
}

async function generateEngagementPost(context: any, personality: SyraPersonality): Promise<{content: string}> {
  const engagementPosts = [
    "Question time! 🤔 What's your favorite Synapse feature and why? Let's discuss! 💬",
    "Poll time! 📊 What type of content do you enjoy most on Synapse? React with your favorite! 👍",
    "Shoutout to all the amazing creators on Synapse! 🎉 Your content makes this community incredible! ✨"
  ];
  
  const randomPost = engagementPosts[Math.floor(Math.random() * engagementPosts.length)];
  return { content: randomPost };
}
