'use client';

import { useState } from 'react';

type Msg = { role: 'user' | 'bot'; text: string };

export default function ChatPage() {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  async function send(e: React.FormEvent) {
    e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;

    setMessages(m => [...m, { role: 'user', text }]);
    setInput('');
    setLoading(true);

    try {
      const r = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ message: text }) // под твой формат
      });
      if (!r.ok) throw new Error(await r.text());
      const data = await r.json(); // ожидаем { reply: string }
      setMessages(m => [...m, { role: 'bot', text: data.reply ?? JSON.stringify(data) }]);
    } catch (e: any) {
      setMessages(m => [...m, { role: 'bot', text: `Ошибка: ${e.message}` }]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto max-w-xl p-4">
      <h1 className="text-2xl font-bold mb-4">MAX Mini-App Chat</h1>

      <div className="border rounded-lg p-3 h-[60vh] overflow-y-auto mb-3 bg-gray-50">
        {messages.length === 0 && (
          <p className="text-sm text-gray-500">
            Напиши сообщение — я отправлю его на бэкенд и покажу ответ.
          </p>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`my-2 ${m.role === 'user' ? 'text-right' : 'text-left'}`}>
            <span className={`inline-block px-3 py-2 rounded-2xl ${
              m.role === 'user' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-black'
            }`}>
              {m.text}
            </span>
          </div>
        ))}
        {loading && <div className="text-sm text-gray-500 mt-2">Отправляю…</div>}
      </div>

      <form onSubmit={send} className="flex gap-2">
        <input
          className="flex-1 border rounded-lg px-3 py-2"
          placeholder="Сообщение…"
          value={input}
          onChange={e => setInput(e.target.value)}
        />
        <button
          className="px-4 py-2 rounded-lg bg-black text-white disabled:opacity-60"
          disabled={loading || !input.trim()}
        >
          Отправить
        </button>
      </form>
    </main>
  );
}
