import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface ModerationRequest {
  content: string;
  content_id: string;
  content_type: 'post' | 'message' | 'comment' | 'profile';
  user_id: string;
}

interface ModerationResult {
  toxicity: number;
  spam: number;
  harassment: number;
  hate_speech: number;
  adult_content: number;
}

async function analyzeContent(content: string): Promise<ModerationResult> {
  // Simple keyword-based analysis (replace with actual AI service)
  const toxicWords = ['hate', 'stupid', 'idiot', 'kill', 'die'];
  const spamWords = ['buy now', 'click here', 'free money', 'limited time'];
  const harassmentWords = ['loser', 'ugly', 'worthless', 'pathetic'];
  const hateWords = ['racist', 'nazi', 'terrorist'];
  const adultWords = ['sex', 'porn', 'nude', 'xxx'];
  
  const lowerContent = content.toLowerCase();
  
  return {
    toxicity: toxicWords.some(word => lowerContent.includes(word)) ? 0.85 : 0.1,
    spam: spamWords.some(word => lowerContent.includes(word)) ? 0.75 : 0.05,
    harassment: harassmentWords.some(word => lowerContent.includes(word)) ? 0.8 : 0.1,
    hate_speech: hateWords.some(word => lowerContent.includes(word)) ? 0.9 : 0.05,
    adult_content: adultWords.some(word => lowerContent.includes(word)) ? 0.7 : 0.1
  };
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { content, content_id, content_type, user_id }: ModerationRequest = await req.json();
    
    // Analyze content
    const scores = await analyzeContent(content);
    
    // Get active moderation rules
    const { data: rules } = await supabase
      .from('moderation_rules')
      .select('*')
      .eq('is_active', true);
    
    const violations = [];
    
    // Check each rule
    for (const rule of rules || []) {
      const score = scores[rule.rule_type as keyof ModerationResult];
      if (score >= rule.threshold_score) {
        violations.push({
          rule_id: rule.id,
          confidence_score: score,
          action: rule.action
        });
      }
    }
    
    // Flag content if violations found
    if (violations.length > 0) {
      for (const violation of violations) {
        await supabase.from('flagged_content').insert({
          content_id,
          content_type,
          user_id,
          rule_id: violation.rule_id,
          confidence_score: violation.confidence_score,
          flagged_by: 'ai_system'
        });
      }
    }
    
    return new Response(JSON.stringify({
      flagged: violations.length > 0,
      violations: violations.length,
      actions: violations.map(v => v.action),
      scores
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
