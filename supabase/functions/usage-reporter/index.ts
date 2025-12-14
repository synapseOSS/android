import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface ReportRequest {
  type: 'daily' | 'weekly' | 'monthly' | 'custom';
  start_date?: string;
  end_date?: string;
  user_id?: string;
  format?: 'json' | 'csv';
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { type, start_date, end_date, user_id, format = 'json' }: ReportRequest = await req.json();

    const dateRange = getDateRange(type, start_date, end_date);
    const report = await generateReport(dateRange, user_id);

    if (format === 'csv') {
      return new Response(convertToCSV(report), {
        headers: {
          'Content-Type': 'text/csv',
          'Content-Disposition': `attachment; filename="usage-report-${type}.csv"`
        }
      });
    }

    return new Response(JSON.stringify({
      success: true,
      report_type: type,
      period: dateRange,
      data: report,
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

function getDateRange(type: string, start_date?: string, end_date?: string) {
  const now = new Date();
  
  if (type === 'custom' && start_date && end_date) {
    return { start: start_date, end: end_date };
  }

  const ranges = {
    daily: {
      start: new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString(),
      end: now.toISOString()
    },
    weekly: {
      start: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString(),
      end: now.toISOString()
    },
    monthly: {
      start: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString(),
      end: now.toISOString()
    }
  };

  return ranges[type as keyof typeof ranges] || ranges.daily;
}

async function generateReport(dateRange: any, user_id?: string) {
  // Get usage analytics
  let usageQuery = supabase
    .from('ai_usage_analytics')
    .select('*')
    .gte('timestamp', dateRange.start)
    .lte('timestamp', dateRange.end);

  if (user_id) usageQuery = usageQuery.eq('user_id', user_id);

  const { data: usageData } = await usageQuery;

  // Get performance metrics
  let perfQuery = supabase
    .from('performance_metrics')
    .select('*')
    .gte('created_at', dateRange.start)
    .lte('created_at', dateRange.end);

  const { data: perfData } = await perfQuery;

  // Get engagement data
  let engagementQuery = supabase
    .from('user_engagement_analytics')
    .select('*')
    .gte('created_at', dateRange.start)
    .lte('created_at', dateRange.end);

  if (user_id) engagementQuery = engagementQuery.eq('user_id', user_id);

  const { data: engagementData } = await engagementQuery;

  return {
    summary: {
      total_requests: usageData?.length || 0,
      total_tokens: usageData?.reduce((sum, record) => sum + (record.tokens_used || 0), 0) || 0,
      total_cost: usageData?.reduce((sum, record) => sum + (record.cost || 0), 0) || 0,
      avg_response_time: perfData?.reduce((sum, record) => sum + record.response_time, 0) / (perfData?.length || 1) || 0,
      success_rate: perfData?.reduce((sum, record) => sum + record.success_rate, 0) / (perfData?.length || 1) || 1,
      unique_users: new Set(usageData?.map(record => record.user_id)).size || 0
    },
    usage_by_provider: usageData?.reduce((acc: any, record) => {
      if (!acc[record.provider]) {
        acc[record.provider] = { requests: 0, tokens: 0, cost: 0 };
      }
      acc[record.provider].requests++;
      acc[record.provider].tokens += record.tokens_used || 0;
      acc[record.provider].cost += record.cost || 0;
      return acc;
    }, {}) || {},
    usage_by_function: usageData?.reduce((acc: any, record) => {
      if (!acc[record.function_name]) {
        acc[record.function_name] = { calls: 0, avg_response_time: 0 };
      }
      acc[record.function_name].calls++;
      return acc;
    }, {}) || {},
    performance_trends: perfData?.map(record => ({
      timestamp: record.created_at,
      function_name: record.function_name,
      response_time: record.response_time,
      success_rate: record.success_rate
    })) || [],
    engagement_metrics: {
      avg_satisfaction: engagementData?.reduce((sum, record) => sum + (record.satisfaction_score || 0), 0) / (engagementData?.length || 1) || 0,
      feature_usage: engagementData?.reduce((acc: any, record) => {
        const features = record.feature_usage || {};
        Object.keys(features).forEach(feature => {
          acc[feature] = (acc[feature] || 0) + features[feature];
        });
        return acc;
      }, {}) || {}
    },
    top_errors: perfData?.filter(record => record.error_message)
      .reduce((acc: any, record) => {
        const error = record.error_message;
        acc[error] = (acc[error] || 0) + 1;
        return acc;
      }, {}) || {}
  };
}

function convertToCSV(report: any) {
  const rows = [
    ['Metric', 'Value'],
    ['Total Requests', report.summary.total_requests],
    ['Total Tokens', report.summary.total_tokens],
    ['Total Cost', report.summary.total_cost],
    ['Average Response Time', report.summary.avg_response_time],
    ['Success Rate', report.summary.success_rate],
    ['Unique Users', report.summary.unique_users],
    [''],
    ['Provider Usage', ''],
    ...Object.entries(report.usage_by_provider).map(([provider, data]: [string, any]) => 
      [provider, `${data.requests} requests, ${data.tokens} tokens, $${data.cost.toFixed(4)}`]
    )
  ];

  return rows.map(row => row.join(',')).join('\n');
}
