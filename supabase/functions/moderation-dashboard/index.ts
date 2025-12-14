import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const path = url.pathname.split('/').pop();
  
  try {
    switch (path) {
      case 'flagged':
        // GET /flagged - List flagged content
        const status = url.searchParams.get('status') || 'pending';
        const limit = parseInt(url.searchParams.get('limit') || '20');
        
        const { data: flaggedContent } = await supabase
          .from('flagged_content')
          .select(`
            *,
            users!flagged_content_user_id_fkey(username, display_name),
            moderation_rules(rule_name, rule_type, action)
          `)
          .eq('status', status)
          .order('created_at', { ascending: false })
          .limit(limit);
        
        return new Response(JSON.stringify(flaggedContent), {
          headers: { 'Content-Type': 'application/json' }
        });
        
      case 'stats':
        // GET /stats - Moderation statistics
        const [
          { count: totalFlagged },
          { count: pendingCount },
          { count: reviewedCount }
        ] = await Promise.all([
          supabase.from('flagged_content').select('*', { count: 'exact', head: true }),
          supabase.from('flagged_content').select('*', { count: 'exact', head: true }).eq('status', 'pending'),
          supabase.from('flagged_content').select('*', { count: 'exact', head: true }).eq('status', 'reviewed')
        ]);
        
        return new Response(JSON.stringify({
          total_flagged: totalFlagged,
          pending: pendingCount,
          reviewed: reviewedCount,
          auto_actioned: totalFlagged - pendingCount - reviewedCount
        }), {
          headers: { 'Content-Type': 'application/json' }
        });
        
      case 'rules':
        // GET /rules - List moderation rules
        const { data: rules } = await supabase
          .from('moderation_rules')
          .select('*')
          .order('severity_level', { ascending: false });
        
        return new Response(JSON.stringify(rules), {
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
