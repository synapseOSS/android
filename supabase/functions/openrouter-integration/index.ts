import "jsr:@supabase/functions-js/edge-runtime.d.ts";

interface OpenRouterRequest {
  model: string;
  messages: Array<{role: string, content: string}>;
  max_tokens?: number;
  temperature?: number;
}

interface OpenRouterResponse {
  choices: Array<{
    message: {content: string};
    finish_reason: string;
  }>;
  usage: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  };
}

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  try {
    const { api_key, model = "openai/gpt-3.5-turbo", messages, max_tokens = 1000, temperature = 0.7 } = await req.json();

    if (!api_key) {
      return new Response('API key required', { status: 400 });
    }

    const openRouterRequest: OpenRouterRequest = {
      model,
      messages,
      max_tokens,
      temperature
    };

    const response = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${api_key}`,
        'Content-Type': 'application/json',
        'HTTP-Referer': 'https://synapse.social',
        'X-Title': 'Synapse Social'
      },
      body: JSON.stringify(openRouterRequest)
    });

    if (!response.ok) {
      const error = await response.text();
      return new Response(JSON.stringify({ 
        error: `OpenRouter API error: ${error}` 
      }), { 
        status: response.status,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    const data: OpenRouterResponse = await response.json();
    
    return new Response(JSON.stringify({
      success: true,
      response: data.choices[0]?.message?.content || '',
      tokens_used: data.usage?.total_tokens || 0,
      model_used: model,
      provider: 'openrouter'
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({ 
      error: error.message 
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});
