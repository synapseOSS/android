import "jsr:@supabase/functions-js/edge-runtime.d.ts";

Deno.serve(async (req: Request) => {
  try {
    const { messageText } = await req.json();
    
    const geminiApiKey = Deno.env.get('GEMINI_API_KEY');
    
    if (!geminiApiKey) {
      return new Response(JSON.stringify({
        error: "GEMINI_API_KEY environment variable not set",
        hasKey: false,
        keyLength: 0
      }));
    }
    
    const systemPrompt = `You are Syra, a friendly AI assistant in the Synapse social media app. You're helpful, engaging, and use emojis naturally. Keep responses concise (1-2 sentences max).`;
    const prompt = `${systemPrompt}\n\nUser message: "${messageText}"\n\nRespond as Syra:`;

    try {
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          contents: [{
            parts: [{
              text: prompt
            }]
          }],
          generationConfig: {
            temperature: 0.7,
            topK: 40,
            topP: 0.95,
            maxOutputTokens: 100,
          }
        })
      });

      const responseText = await response.text();
      
      if (!response.ok) {
        return new Response(JSON.stringify({
          error: "Gemini API error",
          status: response.status,
          statusText: response.statusText,
          response: responseText,
          hasKey: true,
          keyLength: geminiApiKey.length
        }));
      }

      const data = JSON.parse(responseText);
      const aiResponse = data.candidates?.[0]?.content?.parts?.[0]?.text;
      
      return new Response(JSON.stringify({
        success: true,
        response: aiResponse || "No response generated",
        hasKey: true,
        keyLength: geminiApiKey.length,
        rawData: data
      }));
      
    } catch (fetchError) {
      return new Response(JSON.stringify({
        error: "Fetch error",
        message: fetchError.message,
        hasKey: true,
        keyLength: geminiApiKey.length
      }));
    }
    
  } catch (error) {
    return new Response(JSON.stringify({
      error: error.message
    }), { status: 400 });
  }
});
