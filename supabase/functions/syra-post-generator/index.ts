import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface PostRequest {
  user_id: string;
  topic?: string;
  mood?: string;
  content_type?: 'text' | 'image_caption' | 'poll' | 'story';
  target_audience?: string;
}

const SYRA_PERSONALITY = {
  tone: "friendly, witty, and engaging",
  style: "conversational with subtle humor",
  expertise: ["social trends", "lifestyle", "technology", "creativity"],
  voice: "authentic millennial with Gen-Z awareness"
};

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { user_id, topic, mood = 'casual', content_type = 'text', target_audience }: PostRequest = await req.json();

    if (!user_id) {
      return new Response('Missing user_id', { status: 400 });
    }

    // Get user context
    const { data: user } = await supabase
      .from('users')
      .select('username, bio, interests')
      .eq('id', user_id)
      .single();

    // Generate post content
    const prompt = `As Syra, create a ${content_type} post about ${topic || 'trending topics'} with ${mood} mood for ${target_audience || 'general audience'}. 
    User context: ${user?.username} - ${user?.bio}
    Personality: ${SYRA_PERSONALITY.tone}, ${SYRA_PERSONALITY.style}
    Keep it under 280 characters, engaging, and authentic.`;

    // Simulate AI generation (replace with actual AI service)
    const generatedContent = await generateContent(prompt, content_type);

    // Save to database
    const { data: post_draft } = await supabase
      .from('ai_post_drafts')
      .insert({
        user_id,
        generated_content: generatedContent.content,
        content_type,
        topic,
        mood,
        ai_suggestions: generatedContent.suggestions,
        engagement_score: generatedContent.engagement_score
      })
      .select()
      .single();

    return new Response(JSON.stringify({
      success: true,
      post_draft,
      syra_advice: generatedContent.advice
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

async function generateContent(prompt: string, type: string) {
  // Mock AI generation - replace with actual AI service
  const templates = {
    text: [
      "Just discovered something amazing! ✨ {topic} is totally changing the game. Who else is obsessed? 🤔",
      "Hot take: {topic} hits different when you really understand it 💯 Thoughts?",
      "Currently vibing with {topic} and honestly? Best decision ever 🔥"
    ],
    image_caption: [
      "When {topic} meets perfect timing ✨ #mood",
      "This {topic} energy is everything 💫",
      "Caught in the moment with {topic} 📸"
    ]
  };

  const content = templates[type]?.[Math.floor(Math.random() * templates[type].length)] || templates.text[0];
  
  return {
    content: content.replace('{topic}', prompt.split('about ')[1]?.split(' with')[0] || 'life'),
    suggestions: ['Add trending hashtags', 'Include call-to-action', 'Tag relevant users'],
    engagement_score: Math.floor(Math.random() * 40) + 60, // 60-100
    advice: "This post has great engagement potential! Consider posting during peak hours (7-9 PM) for maximum reach."
  };
}
