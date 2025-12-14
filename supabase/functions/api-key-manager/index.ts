import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from 'jsr:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

// AES-256-GCM encryption using Web Crypto API
const ENCRYPTION_KEY_BASE64 = Deno.env.get('API_ENCRYPTION_KEY') ?? 'YourBase64EncodedKeyHere==';

async function getEncryptionKey(): Promise<CryptoKey> {
  const keyData = new Uint8Array(atob(ENCRYPTION_KEY_BASE64).split('').map(c => c.charCodeAt(0)));
  return await crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'AES-GCM' },
    false,
    ['encrypt', 'decrypt']
  );
}

async function encryptKey(key: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(key);
  const cryptoKey = await getEncryptionKey();
  const iv = crypto.getRandomValues(new Uint8Array(12));
  
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    cryptoKey,
    data
  );
  
  // Combine IV and encrypted data
  const combined = new Uint8Array(iv.length + encrypted.byteLength);
  combined.set(iv);
  combined.set(new Uint8Array(encrypted), iv.length);
  
  return btoa(String.fromCharCode(...combined));
}

async function decryptKey(encryptedKey: string): Promise<string> {
  try {
    const combined = new Uint8Array(atob(encryptedKey).split('').map(c => c.charCodeAt(0)));
    const iv = combined.slice(0, 12);
    const encrypted = combined.slice(12);
    
    const cryptoKey = await getEncryptionKey();
    const decrypted = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv },
      cryptoKey,
      encrypted
    );
    
    const decoder = new TextDecoder();
    return decoder.decode(decrypted);
  } catch {
    throw new Error('Invalid encrypted key');
  }
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const method = req.method;
  
  try {
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response('Unauthorized', { status: 401 });
    }

    // Get user from auth token
    const { data: { user } } = await supabase.auth.getUser(authHeader.replace('Bearer ', ''));
    if (!user) {
      return new Response('Invalid token', { status: 401 });
    }

    if (method === 'POST' && url.pathname.includes('/store')) {
      return await storeApiKey(req, user.id);
    } else if (method === 'GET' && url.pathname.includes('/list')) {
      return await listApiKeys(user.id);
    } else if (method === 'DELETE') {
      return await deleteApiKey(req, user.id);
    } else if (method === 'GET' && url.pathname.includes('/decrypt')) {
      return await getDecryptedKey(req, user.id);
    }
    
    return new Response('Not found', { status: 404 });
    
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});

async function storeApiKey(req: Request, userId: string) {
  const { provider, api_key, key_name, usage_limit } = await req.json();
  
  if (!provider || !api_key) {
    return new Response('Missing required fields', { status: 400 });
  }

  // Validate API key format
  if (!validateApiKey(provider, api_key)) {
    return new Response('Invalid API key format', { status: 400 });
  }

  const encryptedKey = await encryptKey(api_key);
  
  const { data, error } = await supabase
    .from('user_api_keys')
    .upsert({
      user_id: userId,
      provider,
      encrypted_key: encryptedKey,
      key_name: key_name || `${provider} Key`,
      usage_limit,
      updated_at: new Date().toISOString()
    })
    .select()
    .single();

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 400 });
  }

  return new Response(JSON.stringify({
    success: true,
    message: 'API key stored securely',
    key_id: data.id
  }), {
    headers: { 'Content-Type': 'application/json' }
  });
}

async function listApiKeys(userId: string) {
  const { data, error } = await supabase
    .from('user_api_keys')
    .select('id, provider, key_name, is_active, usage_limit, usage_count, created_at')
    .eq('user_id', userId)
    .eq('is_active', true);

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 400 });
  }

  return new Response(JSON.stringify({
    success: true,
    api_keys: data
  }), {
    headers: { 'Content-Type': 'application/json' }
  });
}

async function deleteApiKey(req: Request, userId: string) {
  const { key_id } = await req.json();
  
  const { error } = await supabase
    .from('user_api_keys')
    .update({ is_active: false })
    .eq('id', key_id)
    .eq('user_id', userId);

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 400 });
  }

  return new Response(JSON.stringify({
    success: true,
    message: 'API key deleted'
  }), {
    headers: { 'Content-Type': 'application/json' }
  });
}

async function getDecryptedKey(req: Request, userId: string) {
  const url = new URL(req.url);
  const provider = url.searchParams.get('provider');
  
  if (!provider) {
    return new Response('Provider required', { status: 400 });
  }

  const { data, error } = await supabase
    .from('user_api_keys')
    .select('encrypted_key, usage_count, usage_limit')
    .eq('user_id', userId)
    .eq('provider', provider)
    .eq('is_active', true)
    .single();

  if (error || !data) {
    return new Response(JSON.stringify({ 
      has_key: false,
      use_platform_key: true 
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  // Check usage limit
  if (data.usage_limit && data.usage_count >= data.usage_limit) {
    return new Response(JSON.stringify({ 
      has_key: true,
      use_platform_key: true,
      reason: 'Usage limit exceeded'
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  const decryptedKey = await decryptKey(data.encrypted_key);
  
  return new Response(JSON.stringify({
    has_key: true,
    api_key: decryptedKey,
    use_platform_key: false
  }), {
    headers: { 'Content-Type': 'application/json' }
  });
}

function validateApiKey(provider: string, key: string): boolean {
  const patterns = {
    openai: /^sk-[a-zA-Z0-9]{48,}$/,
    gemini: /^AIza[a-zA-Z0-9_-]{35}$/,
    anthropic: /^sk-ant-[a-zA-Z0-9_-]{95,}$/,
    openrouter: /^sk-or-[a-zA-Z0-9_-]{40,}$/
  };
  
  return patterns[provider]?.test(key) ?? true;
}
