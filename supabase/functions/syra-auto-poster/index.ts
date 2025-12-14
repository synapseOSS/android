import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface ScheduleRequest {
  user_id: string;
  content: string;
  scheduled_time?: string;
  auto_generate?: boolean;
  frequency?: 'daily' | 'weekly' | 'custom';
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  
  if (req.method === 'POST') {
    return await schedulePost(req);
  } else if (req.method === 'GET' && url.pathname.includes('/execute')) {
    return await executeScheduledPosts();
  }
  
  return new Response('Method not allowed', { status: 405 });
});

async function schedulePost(req: Request) {
  try {
    const { user_id, content, scheduled_time, auto_generate = false, frequency }: ScheduleRequest = await req.json();

    if (!user_id) {
      return new Response('Missing user_id', { status: 400 });
    }

    let postContent = content;
    
    // Auto-generate content if requested
    if (auto_generate) {
      const generated = await generateSyraContent(user_id);
      postContent = generated.content;
    }

    // Schedule the post
    const { data: scheduled_post } = await supabase
      .from('scheduled_posts')
      .insert({
        user_id,
        content: postContent,
        scheduled_for: scheduled_time || getOptimalPostTime(),
        status: 'scheduled',
        auto_generated: auto_generate,
        frequency
      })
      .select()
      .single();

    return new Response(JSON.stringify({
      success: true,
      scheduled_post,
      message: auto_generate ? 'Syra generated and scheduled your post!' : 'Post scheduled successfully!'
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}

async function executeScheduledPosts() {
  try {
    const now = new Date().toISOString();
    
    // Get posts ready to publish
    const { data: posts } = await supabase
      .from('scheduled_posts')
      .select('*')
      .eq('status', 'scheduled')
      .lte('scheduled_for', now);

    const results = [];
    
    for (const post of posts || []) {
      try {
        // Create the actual post
        const { data: newPost } = await supabase
          .from('posts')
          .insert({
            user_id: post.user_id,
            content: post.content,
            created_at: new Date().toISOString(),
            is_syra_generated: post.auto_generated
          })
          .select()
          .single();

        // Update scheduled post status
        await supabase
          .from('scheduled_posts')
          .update({ 
            status: 'published',
            published_post_id: newPost.id,
            published_at: new Date().toISOString()
          })
          .eq('id', post.id);

        // Track analytics
        await supabase
          .from('syra_posting_analytics')
          .insert({
            user_id: post.user_id,
            post_id: newPost.id,
            scheduled_post_id: post.id,
            posting_time: new Date().toISOString(),
            auto_generated: post.auto_generated
          });

        results.push({ post_id: newPost.id, status: 'published' });
        
      } catch (error) {
        // Mark as failed
        await supabase
          .from('scheduled_posts')
          .update({ status: 'failed', error_message: error.message })
          .eq('id', post.id);
          
        results.push({ post_id: post.id, status: 'failed', error: error.message });
      }
    }

    return new Response(JSON.stringify({
      success: true,
      processed: results.length,
      results
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}

async function generateSyraContent(user_id: string) {
  // Get user interests and recent activity
  const { data: user } = await supabase
    .from('users')
    .select('interests, username')
    .eq('id', user_id)
    .single();

  const topics = ['motivation', 'lifestyle', 'technology', 'creativity', 'wellness'];
  const randomTopic = topics[Math.floor(Math.random() * topics.length)];
  
  const templates = [
    `Monday motivation: ${randomTopic} is the key to unlocking your potential! 💪 What's inspiring you today?`,
    `Just thinking about how ${randomTopic} shapes our daily lives... Mind = blown 🤯 Anyone else fascinated by this?`,
    `Weekend vibes: Time to dive deep into ${randomTopic} and see where it takes us! ✨ Who's joining the journey?`
  ];
  
  return {
    content: templates[Math.floor(Math.random() * templates.length)],
    topic: randomTopic
  };
}

function getOptimalPostTime(): string {
  const now = new Date();
  const optimal = new Date(now);
  optimal.setHours(19, 30, 0, 0); // 7:30 PM default
  
  // If past optimal time today, schedule for tomorrow
  if (now > optimal) {
    optimal.setDate(optimal.getDate() + 1);
  }
  
  return optimal.toISOString();
}
