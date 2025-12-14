import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

interface FineTuningRequest {
  action: 'create' | 'train' | 'status' | 'deploy' | 'delete';
  user_id: string;
  model_name?: string;
  base_model?: string;
  training_data?: Array<{prompt: string, response: string, rating?: number}>;
  job_id?: string;
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { action, user_id, model_name, base_model, training_data, job_id }: FineTuningRequest = await req.json();

    let result;
    switch (action) {
      case 'create':
        result = await createFineTuningJob(user_id, model_name!, base_model!, training_data!);
        break;
      case 'train':
        result = await startTraining(job_id!);
        break;
      case 'status':
        result = await getTrainingStatus(job_id!);
        break;
      case 'deploy':
        result = await deployModel(job_id!);
        break;
      case 'delete':
        result = await deleteModel(job_id!);
        break;
      default:
        throw new Error('Invalid action');
    }

    return new Response(JSON.stringify({
      success: true,
      action,
      data: result
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

async function createFineTuningJob(user_id: string, model_name: string, base_model: string, training_data: any[]) {
  // Validate training data
  if (!training_data || training_data.length < 10) {
    throw new Error('Minimum 10 training examples required');
  }

  // Process and validate training examples
  const processedData = training_data.map((item, index) => ({
    id: `${user_id}_${index}`,
    prompt: item.prompt.trim(),
    response: item.response.trim(),
    rating: item.rating || 5,
    created_at: new Date().toISOString()
  }));

  // Create fine-tuning job record
  const jobId = `ft_${user_id}_${Date.now()}`;
  
  const { error: jobError } = await supabase
    .from('fine_tuning_jobs')
    .insert({
      job_id: jobId,
      user_id,
      model_name,
      base_model,
      status: 'created',
      training_examples: processedData.length,
      created_at: new Date().toISOString()
    });

  if (jobError) throw jobError;

  // Store training data
  const { error: dataError } = await supabase
    .from('fine_tuning_data')
    .insert(processedData.map(item => ({
      job_id: jobId,
      prompt: item.prompt,
      response: item.response,
      rating: item.rating,
      created_at: item.created_at
    })));

  if (dataError) throw dataError;

  // Generate user preferences from training data
  const preferences = await analyzeUserPreferences(processedData);
  
  const { error: prefError } = await supabase
    .from('user_ai_preferences')
    .upsert({
      user_id,
      preferences,
      updated_at: new Date().toISOString()
    });

  if (prefError) throw prefError;

  return {
    job_id: jobId,
    status: 'created',
    training_examples: processedData.length,
    estimated_time: Math.ceil(processedData.length / 10) + ' minutes',
    preferences
  };
}

async function startTraining(job_id: string) {
  // Update job status
  const { error } = await supabase
    .from('fine_tuning_jobs')
    .update({
      status: 'training',
      started_at: new Date().toISOString()
    })
    .eq('job_id', job_id);

  if (error) throw error;

  // Simulate training process (in real implementation, this would call external API)
  setTimeout(async () => {
    await supabase
      .from('fine_tuning_jobs')
      .update({
        status: 'completed',
        completed_at: new Date().toISOString(),
        model_id: `${job_id}_model`
      })
      .eq('job_id', job_id);
  }, 5000);

  return {
    job_id,
    status: 'training',
    message: 'Training started successfully'
  };
}

async function getTrainingStatus(job_id: string) {
  const { data, error } = await supabase
    .from('fine_tuning_jobs')
    .select('*')
    .eq('job_id', job_id)
    .single();

  if (error) throw error;

  return {
    job_id,
    status: data.status,
    progress: calculateProgress(data),
    created_at: data.created_at,
    started_at: data.started_at,
    completed_at: data.completed_at,
    model_id: data.model_id
  };
}

async function deployModel(job_id: string) {
  const { data, error } = await supabase
    .from('fine_tuning_jobs')
    .select('*')
    .eq('job_id', job_id)
    .single();

  if (error) throw error;

  if (data.status !== 'completed') {
    throw new Error('Model training not completed');
  }

  // Deploy model
  const { error: deployError } = await supabase
    .from('fine_tuning_jobs')
    .update({
      status: 'deployed',
      deployed_at: new Date().toISOString()
    })
    .eq('job_id', job_id);

  if (deployError) throw deployError;

  return {
    job_id,
    model_id: data.model_id,
    status: 'deployed',
    endpoint: `https://api.synapse.com/v1/models/${data.model_id}`
  };
}

async function deleteModel(job_id: string) {
  const { error } = await supabase
    .from('fine_tuning_jobs')
    .update({
      status: 'deleted',
      deleted_at: new Date().toISOString()
    })
    .eq('job_id', job_id);

  if (error) throw error;

  return {
    job_id,
    status: 'deleted',
    message: 'Model deleted successfully'
  };
}

function analyzeUserPreferences(training_data: any[]) {
  const preferences = {
    response_style: 'balanced',
    tone: 'friendly',
    length: 'medium',
    technical_level: 'intermediate',
    topics: [] as string[],
    patterns: {} as any
  };

  // Analyze response lengths
  const avgLength = training_data.reduce((sum, item) => sum + item.response.length, 0) / training_data.length;
  preferences.length = avgLength < 100 ? 'short' : avgLength > 300 ? 'long' : 'medium';

  // Analyze tone from high-rated responses
  const highRated = training_data.filter(item => item.rating >= 4);
  const toneWords = {
    friendly: ['thanks', 'please', 'great', 'awesome'],
    formal: ['however', 'therefore', 'furthermore'],
    casual: ['yeah', 'cool', 'nice', 'hey']
  };

  let toneScores = { friendly: 0, formal: 0, casual: 0 };
  highRated.forEach(item => {
    const text = item.response.toLowerCase();
    Object.entries(toneWords).forEach(([tone, words]) => {
      words.forEach(word => {
        if (text.includes(word)) toneScores[tone as keyof typeof toneScores]++;
      });
    });
  });

  preferences.tone = Object.entries(toneScores).reduce((a, b) => toneScores[a[0] as keyof typeof toneScores] > toneScores[b[0] as keyof typeof toneScores] ? a : b)[0];

  return preferences;
}

function calculateProgress(job: any) {
  if (job.status === 'created') return 0;
  if (job.status === 'training') return 50;
  if (job.status === 'completed') return 100;
  return 0;
}
