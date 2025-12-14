import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface ActionRequest {
  flagged_content_id: string;
  action?: string; // Override action if needed
  moderator_id?: string;
}

async function executeAction(flaggedContent: any, action: string, moderatorId?: string) {
  const { content_id, content_type, user_id } = flaggedContent;
  
  switch (action) {
    case 'hide':
      // Hide content by updating visibility
      if (content_type === 'post') {
        await supabase.from('posts').update({ is_hidden: true }).eq('id', content_id);
      } else if (content_type === 'message') {
        await supabase.from('messages').update({ is_hidden: true }).eq('id', content_id);
      }
      break;
      
    case 'remove':
      // Soft delete content
      if (content_type === 'post') {
        await supabase.from('posts').update({ deleted_at: new Date().toISOString() }).eq('id', content_id);
      } else if (content_type === 'message') {
        await supabase.from('messages').update({ deleted_at: new Date().toISOString() }).eq('id', content_id);
      }
      break;
      
    case 'warn':
      // Send warning notification to user
      await supabase.from('notifications').insert({
        user_id,
        type: 'moderation_warning',
        title: 'Content Warning',
        message: 'Your content has been flagged for violating community guidelines.',
        data: { content_id, content_type }
      });
      break;
      
    case 'escalate':
      // Mark for human review
      await supabase.from('flagged_content')
        .update({ status: 'escalated' })
        .eq('id', flaggedContent.id);
      break;
  }
  
  // Log action in moderation history
  await supabase.from('moderation_history').insert({
    flagged_content_id: flaggedContent.id,
    action_taken: action,
    performed_by: moderatorId || null,
    reason: moderatorId ? 'Manual action' : 'Automated action'
  });
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { flagged_content_id, action, moderator_id }: ActionRequest = await req.json();
    
    // Get flagged content details
    const { data: flaggedContent } = await supabase
      .from('flagged_content')
      .select(`
        *,
        moderation_rules(action, rule_name)
      `)
      .eq('id', flagged_content_id)
      .single();
    
    if (!flaggedContent) {
      return new Response('Flagged content not found', { status: 404 });
    }
    
    // Use provided action or default from rule
    const actionToTake = action || flaggedContent.moderation_rules.action;
    
    // Execute the action
    await executeAction(flaggedContent, actionToTake, moderator_id);
    
    // Update flagged content status
    await supabase.from('flagged_content').update({
      status: moderator_id ? 'reviewed' : 'auto_actioned',
      moderator_id,
      moderator_action: actionToTake,
      reviewed_at: new Date().toISOString()
    }).eq('id', flagged_content_id);
    
    return new Response(JSON.stringify({
      success: true,
      action_taken: actionToTake,
      content_id: flaggedContent.content_id,
      content_type: flaggedContent.content_type
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
