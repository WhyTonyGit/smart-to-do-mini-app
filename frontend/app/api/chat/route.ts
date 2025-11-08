export const runtime = 'edge';

export async function POST(req: Request) {
  const body = await req.json();
  const url = `${process.env.BACKEND_URL}/chat`; // подстрой под свой путь

  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      ...(process.env.BACKEND_TOKEN
        ? { authorization: `Bearer ${process.env.BACKEND_TOKEN}` }
        : {})
    },
    body: JSON.stringify(body),
  });

  // пробрасываем ответ как есть (поддержит JSON/стрим и т.п.)
  return new Response(res.body, {
    status: res.status,
    headers: { 'content-type': res.headers.get('content-type') ?? 'application/json' }
  });
}
