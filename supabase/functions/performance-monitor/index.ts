import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface PerformanceMetric {
  function_name: string;
  response_time: number;
  memory_usage?: number;
  success_rate: number;
  error_message?: string;
  user_id?: string;
  timestamp: string;
}

Deno.serve(async (req: Request) => {
  if (req.method === 'POST') {
    return await recordMetric(req);
  } else if (req.method === 'GET') {
    return await getMetrics(req);
  }
  
  return new Response('Method not allowed', { status: 405 });
});

async function recordMetric(req: Request) {
  try {
    const metric: PerformanceMetric = await req.json();
    
    const { error } = await supabase
      .from('performance_metrics')
      .insert({
        function_name: metric.function_name,
        response_time: metric.response_time,
        memory_usage: metric.memory_usage,
        success_rate: metric.success_rate,
        error_message: metric.error_message,
        user_id: metric.user_id,
        created_at: new Date().toISOString()
      });

    if (error) throw error;

    // Check for performance alerts
    const alerts = await checkPerformanceAlerts(metric);

    return new Response(JSON.stringify({
      success: true,
      alerts: alerts.length > 0 ? alerts : undefined
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
}

async function getMetrics(req: Request) {
  try {
    const url = new URL(req.url);
    const function_name = url.searchParams.get('function');
    const timeframe = url.searchParams.get('timeframe') || 'hour';
    
    const timeRanges = {
      hour: new Date(Date.now() - 60 * 60 * 1000),
      day: new Date(Date.now() - 24 * 60 * 60 * 1000),
      week: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
    };

    let query = supabase
      .from('performance_metrics')
      .select('*')
      .gte('created_at', timeRanges[timeframe as keyof typeof timeRanges].toISOString())
      .order('created_at', { ascending: false });

    if (function_name) {
      query = query.eq('function_name', function_name);
    }

    const { data, error } = await query.limit(100);
    if (error) throw error;

    const metrics = {
      avg_response_time: data?.reduce((sum, m) => sum + m.response_time, 0) / (data?.length || 1) || 0,
      avg_memory_usage: data?.reduce((sum, m) => sum + (m.memory_usage || 0), 0) / (data?.length || 1) || 0,
      success_rate: data?.reduce((sum, m) => sum + m.success_rate, 0) / (data?.length || 1) || 1,
      total_calls: data?.length || 0,
      error_count: data?.filter(m => m.success_rate < 1).length || 0,
      recent_data: data?.slice(0, 20) || []
    };

    return new Response(JSON.stringify({
      success: true,
      timeframe,
      function_name,
      metrics
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
}

async function checkPerformanceAlerts(metric: PerformanceMetric) {
  const alerts = [];

  // High response time alert
  if (metric.response_time > 5000) {
    alerts.push({
      type: 'high_response_time',
      severity: 'warning',
      message: `${metric.function_name} response time: ${metric.response_time}ms`,
      threshold: 5000
    });
  }

  // Low success rate alert
  if (metric.success_rate < 0.95) {
    alerts.push({
      type: 'low_success_rate',
      severity: 'error',
      message: `${metric.function_name} success rate: ${(metric.success_rate * 100).toFixed(1)}%`,
      threshold: 95
    });
  }

  // High memory usage alert
  if (metric.memory_usage && metric.memory_usage > 100) {
    alerts.push({
      type: 'high_memory_usage',
      severity: 'warning',
      message: `${metric.function_name} memory usage: ${metric.memory_usage}MB`,
      threshold: 100
    });
  }

  return alerts;
}
