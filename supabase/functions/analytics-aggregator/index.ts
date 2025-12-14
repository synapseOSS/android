import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface AnalyticsRequest {
  type: 'usage' | 'performance' | 'engagement' | 'cost';
  user_id?: string;
  timeframe?: 'hour' | 'day' | 'week' | 'month';
  provider?: string;
  function_name?: string;
}

interface UsageMetrics {
  total_requests: number;
  total_tokens: number;
  total_cost: number;
  avg_response_time: number;
  success_rate: number;
  top_providers: Array<{provider: string, usage: number}>;
  top_functions: Array<{function_name: string, calls: number}>;
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { type, user_id, timeframe = 'day', provider, function_name }: AnalyticsRequest = await req.json();

    // Calculate time range
    const now = new Date();
    const timeRanges = {
      hour: new Date(now.getTime() - 60 * 60 * 1000),
      day: new Date(now.getTime() - 24 * 60 * 60 * 1000),
      week: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000),
      month: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
    };
    const startTime = timeRanges[timeframe];

    let analytics: any = {};

    switch (type) {
      case 'usage':
        analytics = await getUsageAnalytics(startTime, user_id, provider);
        break;
      case 'performance':
        analytics = await getPerformanceAnalytics(startTime, function_name);
        break;
      case 'engagement':
        analytics = await getEngagementAnalytics(startTime, user_id);
        break;
      case 'cost':
        analytics = await getCostAnalytics(startTime, user_id, provider);
        break;
    }

    return new Response(JSON.stringify({
      success: true,
      type,
      timeframe,
      data: analytics,
      generated_at: new Date().toISOString()
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({
      success: false,
      error: error.message
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});

async function getUsageAnalytics(startTime: Date, user_id?: string, provider?: string): Promise<UsageMetrics> {
  let query = supabase
    .from('ai_usage_analytics')
    .select('*')
    .gte('timestamp', startTime.toISOString());

  if (user_id) query = query.eq('user_id', user_id);
  if (provider) query = query.eq('provider', provider);

  const { data, error } = await query;
  if (error) throw error;

  const totalRequests = data?.length || 0;
  const totalTokens = data?.reduce((sum, record) => sum + (record.tokens_used || 0), 0) || 0;
  const totalCost = data?.reduce((sum, record) => sum + (record.cost || 0), 0) || 0;

  // Get performance data
  const { data: perfData } = await supabase
    .from('performance_metrics')
    .select('response_time, success_rate')
    .gte('created_at', startTime.toISOString());

  const avgResponseTime = perfData?.reduce((sum, record) => sum + record.response_time, 0) / (perfData?.length || 1) || 0;
  const successRate = perfData?.reduce((sum, record) => sum + record.success_rate, 0) / (perfData?.length || 1) || 100;

  // Top providers
  const providerUsage = data?.reduce((acc: any, record) => {
    acc[record.provider] = (acc[record.provider] || 0) + 1;
    return acc;
  }, {}) || {};

  const topProviders = Object.entries(providerUsage)
    .map(([provider, usage]) => ({ provider, usage: usage as number }))
    .sort((a, b) => b.usage - a.usage)
    .slice(0, 5);

  // Top functions
  const functionUsage = data?.reduce((acc: any, record) => {
    acc[record.function_name] = (acc[record.function_name] || 0) + 1;
    return acc;
  }, {}) || {};

  const topFunctions = Object.entries(functionUsage)
    .map(([function_name, calls]) => ({ function_name, calls: calls as number }))
    .sort((a, b) => b.calls - a.calls)
    .slice(0, 5);

  return {
    total_requests: totalRequests,
    total_tokens: totalTokens,
    total_cost: totalCost,
    avg_response_time: Math.round(avgResponseTime),
    success_rate: Math.round(successRate * 100) / 100,
    top_providers: topProviders,
    top_functions: topFunctions
  };
}

async function getPerformanceAnalytics(startTime: Date, function_name?: string) {
  let query = supabase
    .from('performance_metrics')
    .select('*')
    .gte('created_at', startTime.toISOString());

  if (function_name) query = query.eq('function_name', function_name);

  const { data, error } = await query;
  if (error) throw error;

  return {
    total_calls: data?.length || 0,
    avg_response_time: data?.reduce((sum, record) => sum + record.response_time, 0) / (data?.length || 1) || 0,
    avg_memory_usage: data?.reduce((sum, record) => sum + (record.memory_usage || 0), 0) / (data?.length || 1) || 0,
    error_rate: data?.filter(record => record.success_rate < 1).length / (data?.length || 1) * 100 || 0,
    performance_trend: data?.slice(-10).map(record => ({
      timestamp: record.created_at,
      response_time: record.response_time,
      success_rate: record.success_rate
    })) || []
  };
}

async function getEngagementAnalytics(startTime: Date, user_id?: string) {
  let query = supabase
    .from('user_engagement_analytics')
    .select('*')
    .gte('created_at', startTime.toISOString());

  if (user_id) query = query.eq('user_id', user_id);

  const { data, error } = await query;
  if (error) throw error;

  return {
    total_users: new Set(data?.map(record => record.user_id)).size || 0,
    avg_satisfaction: data?.reduce((sum, record) => sum + (record.satisfaction_score || 0), 0) / (data?.length || 1) || 0,
    feature_usage: data?.reduce((acc: any, record) => {
      const features = record.feature_usage || {};
      Object.keys(features).forEach(feature => {
        acc[feature] = (acc[feature] || 0) + features[feature];
      });
      return acc;
    }, {}) || {},
    engagement_trend: data?.slice(-10).map(record => ({
      timestamp: record.created_at,
      satisfaction: record.satisfaction_score,
      usage_count: Object.values(record.feature_usage || {}).reduce((sum: number, count) => sum + (count as number), 0)
    })) || []
  };
}

async function getCostAnalytics(startTime: Date, user_id?: string, provider?: string) {
  let query = supabase
    .from('ai_usage_analytics')
    .select('cost, provider, model, timestamp')
    .gte('timestamp', startTime.toISOString());

  if (user_id) query = query.eq('user_id', user_id);
  if (provider) query = query.eq('provider', provider);

  const { data, error } = await query;
  if (error) throw error;

  const totalCost = data?.reduce((sum, record) => sum + (record.cost || 0), 0) || 0;
  
  const costByProvider = data?.reduce((acc: any, record) => {
    acc[record.provider] = (acc[record.provider] || 0) + (record.cost || 0);
    return acc;
  }, {}) || {};

  const costByModel = data?.reduce((acc: any, record) => {
    acc[record.model] = (acc[record.model] || 0) + (record.cost || 0);
    return acc;
  }, {}) || {};

  return {
    total_cost: Math.round(totalCost * 100) / 100,
    cost_by_provider: Object.entries(costByProvider).map(([provider, cost]) => ({
      provider,
      cost: Math.round((cost as number) * 100) / 100
    })),
    cost_by_model: Object.entries(costByModel).map(([model, cost]) => ({
      model,
      cost: Math.round((cost as number) * 100) / 100
    })),
    daily_cost_trend: data?.reduce((acc: any, record) => {
      const date = record.timestamp.split('T')[0];
      acc[date] = (acc[date] || 0) + (record.cost || 0);
      return acc;
    }, {}) || {}
  };
}
