import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const endpoint = url.pathname.split('/').pop();
  
  try {
    switch (endpoint) {
      case 'stats':
        // Overall AI chat statistics
        const [
          { count: totalSessions },
          { count: totalResponses },
          { count: activeSessions },
          { data: avgResponseTime },
          { data: avgRating }
        ] = await Promise.all([
          supabase.from('ai_chat_sessions').select('*', { count: 'exact', head: true }),
          supabase.from('ai_chat_responses').select('*', { count: 'exact', head: true }),
          supabase.from('ai_chat_sessions').select('*', { count: 'exact', head: true }).eq('is_active', true),
          supabase.from('ai_chat_responses').select('response_time_ms').not('response_time_ms', 'is', null),
          supabase.from('ai_chat_responses').select('user_feedback').not('user_feedback', 'is', null)
        ]);
        
        const avgTime = avgResponseTime?.reduce((sum, r) => sum + r.response_time_ms, 0) / (avgResponseTime?.length || 1);
        const avgScore = avgRating?.reduce((sum, r) => sum + r.user_feedback, 0) / (avgRating?.length || 1);
        
        return new Response(JSON.stringify({
          total_sessions: totalSessions,
          total_responses: totalResponses,
          avg_response_time_ms: Math.round(avgTime || 0),
          avg_user_rating: Math.round((avgScore || 0) * 10) / 10,
          active_sessions: activeSessions
        }), {
          headers: { 'Content-Type': 'application/json' }
        });
        
      case 'feedback':
        if (req.method === 'POST') {
          // Submit user feedback
          const { response_id, rating, comment } = await req.json();
          
          await supabase.from('ai_chat_responses')
            .update({ user_feedback: rating })
            .eq('id', response_id);
          
          if (comment) {
            await supabase.from('ai_chat_context').insert({
              session_id: null,
              context_type: 'user_feedback',
              context_content: { response_id, rating, comment }
            });
          }
          
          return new Response(JSON.stringify({ success: true }), {
            headers: { 'Content-Type': 'application/json' }
          });
        }
        break;
        
      case 'performance':
        // Performance metrics by type
        const { data: performance } = await supabase
          .from('ai_chat_responses')
          .select('response_type, response_time_ms, user_feedback, tokens_used')
          .not('response_time_ms', 'is', null);
        
        const metrics = performance?.reduce((acc, r) => {
          if (!acc[r.response_type]) {
            acc[r.response_type] = { count: 0, total_time: 0, total_tokens: 0, ratings: [] };
          }
          acc[r.response_type].count++;
          acc[r.response_type].total_time += r.response_time_ms;
          acc[r.response_type].total_tokens += r.tokens_used || 0;
          if (r.user_feedback) acc[r.response_type].ratings.push(r.user_feedback);
          return acc;
        }, {});
        
        return new Response(JSON.stringify(metrics), {
          headers: { 'Content-Type': 'application/json' }
        });
        
      default:
        return new Response('Not found', { status: 404 });
    }
    
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});
