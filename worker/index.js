/**
 * Workout Tracker — AI Coach proxy worker
 *
 * Sits between the Android app and Groq API.
 * Users never see or need any API key.
 *
 * Secrets set via `wrangler secret put`:
 *   GROQ_API_KEY  — your Groq key (gsk_...)
 *   APP_SECRET    — a random string you invent (the app sends this to prove it's your app)
 */

const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';

export default {
  async fetch(request, env) {
    // Only allow POST
    if (request.method !== 'POST') {
      return json({ error: 'Method not allowed' }, 405);
    }

    // Verify the request is coming from your app
    const secret = request.headers.get('X-App-Secret');
    if (!secret || secret !== env.APP_SECRET) {
      return json({ error: 'Unauthorized' }, 401);
    }

    try {
      const body = await request.text();

      const groqRes = await fetch(GROQ_URL, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${env.GROQ_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body,
      });

      const data = await groqRes.text();
      return new Response(data, {
        status: groqRes.status,
        headers: { 'Content-Type': 'application/json' },
      });
    } catch (err) {
      return json({ error: err.message }, 500);
    }
  },
};

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
