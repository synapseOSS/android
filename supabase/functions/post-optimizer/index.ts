import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface OptimizeRequest {
  user_id: string;
  content: string;
  post_type?: 'text' | 'image' | 'video' | 'poll';
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { user_id, content, post_type = 'text' }: OptimizeRequest = await req.json();

    if (!user_id || !content) {
      return new Response('Missing required fields', { status: 400 });
    }

    // Analyze content and generate optimizations
    const optimization = await optimizeContent(content, post_type);

    // Save optimization history
    await supabase
      .from('ai_post_optimizations')
      .insert({
        user_id,
        original_content: content,
        optimized_content: optimization.enhanced_content,
        hashtag_suggestions: optimization.hashtags,
        engagement_prediction: optimization.engagement_score,
        optimization_type: post_type
      });

    return new Response(JSON.stringify({
      success: true,
      original_content: content,
      enhanced_content: optimization.enhanced_content,
      hashtags: optimization.hashtags,
      engagement_score: optimization.engagement_score,
      syra_tips: optimization.tips,
      best_posting_time: optimization.optimal_time
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

async function optimizeContent(content: string, type: string) {
  // Content analysis and enhancement
  const wordCount = content.split(' ').length;
  const hasEmojis = /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]/u.test(content);
  const hasHashtags = content.includes('#');
  
  let enhanced = content;
  let score = 50;
  
  // Enhance content based on analysis
  if (!hasEmojis && type === 'text') {
    enhanced += ' ✨';
    score += 10;
  }
  
  if (wordCount < 10) {
    score += 15; // Short posts perform better
  }
  
  // Generate hashtags
  const hashtags = generateHashtags(content, type);
  if (hashtags.length > 0) score += 20;
  
  // Optimal posting time (mock)
  const optimalTimes = ['7:00 PM', '8:30 PM', '12:00 PM', '6:00 PM'];
  const optimal_time = optimalTimes[Math.floor(Math.random() * optimalTimes.length)];
  
  return {
    enhanced_content: enhanced,
    hashtags,
    engagement_score: Math.min(score, 95),
    tips: [
      'Add a question to boost engagement',
      'Use trending hashtags for visibility',
      'Post during peak hours for maximum reach'
    ],
    optimal_time
  };
}

function generateHashtags(content: string, type: string): string[] {
  const keywords = content.toLowerCase().split(' ').filter(word => word.length > 3);
  const trendingTags = ['#viral', '#trending', '#mood', '#vibes', '#aesthetic', '#lifestyle'];
  const typeTags = {
    text: ['#thoughts', '#share', '#community'],
    image: ['#photography', '#moment', '#capture'],
    video: ['#video', '#content', '#creative'],
    poll: ['#poll', '#question', '#engage']
  };
  
  const suggested = [...(typeTags[type] || []), ...trendingTags.slice(0, 2)];
  return suggested.slice(0, 5);
}
