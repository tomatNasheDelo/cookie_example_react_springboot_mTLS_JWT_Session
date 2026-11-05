import { useState } from 'react'
import type { FormEvent } from 'react'

const base = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const api = (path: string) => `${base}${path}`
type AuthResult = { message: string; username: string }
type TokenResult = { accessToken: string; tokenType: string; expiresInSeconds: number }
type MtlsResult = { message: string; clientCertificateRequired: boolean }
const credentials = { username: 'student', password: 'change-me-local' }

export default function App() {
  const [jwt, setJwt] = useState('')
  const [sessionResult, setSessionResult] = useState('')
  const [jwtResult, setJwtResult] = useState('')
  const [mtlsResult, setMtlsResult] = useState('')
  const [error, setError] = useState('')

  async function call<T>(path: string, method = 'GET', body?: unknown, bearer?: string): Promise<T> {
    const response = await fetch(api(path), {
      method,
      credentials: 'include',
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
    })
    if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`)
    return await response.json() as T
  }

  async function run(action: () => Promise<void>) {
    setError('')
    try { await action() } catch (reason) { setError(reason instanceof Error ? reason.message : 'Запрос не выполнен') }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run(async () => {
      const session = await call<AuthResult>('/api/auth/session/login', 'POST', credentials)
      setSessionResult(session.message)
      const token = await call<TokenResult>('/api/auth/jwt/login', 'POST', credentials)
      setJwt(token.accessToken)
      setJwtResult(`JWT выдан на ${token.expiresInSeconds} секунд и хранится только в памяти страницы.`)
    })
  }

  return <main className="wrap">
    <header><p className="eyebrow">COOKIE · AUTH · TRANSPORT SECURITY</p><h1>Identity <span>Lab</span></h1><p>Один demo-login, три разных механизма: server session, JWT и взаимный TLS между backend-сервисами.</p></header>
    <section className="panel login"><div><small>ДЕМО УЧЁТНЫЕ ДАННЫЕ</small><p><code>student / change-me-local</code></p><p>Нажми один раз, чтобы создать session cookie и получить JWT.</p></div><form onSubmit={onSubmit}><button>Войти в демо →</button></form></section>
    <div className="grid">
      <section className="panel"><small>01 / SESSION</small><h2>Сессия на сервере</h2><p>Spring хранит user в HttpSession; браузеру выдаётся HttpOnly JSESSIONID. Запрос отправляет cookie через credentials: include.</p><div className="actions"><button className="secondary" onClick={() => void run(async () => { const r = await call<AuthResult>('/api/auth/session/me'); setSessionResult(`${r.message}: ${r.username}`) })}>Проверить сессию</button><button className="quiet" onClick={() => void run(async () => { const r = await call<AuthResult>('/api/auth/session/logout', 'POST'); setSessionResult(r.message) })}>Выйти</button></div><output>{sessionResult || 'Сессия ещё не проверена'}</output><p className="hint">В DevTools → Application/Storage → Cookies ищи HttpOnly JSESSIONID. Не читается через document.cookie.</p></section>
      <section className="panel"><small>02 / JWT</small><h2>Токен у клиента</h2><p>Backend подписывает JWT; frontend хранит его в React state и передаёт как Authorization: Bearer. Перезагрузка страницы удалит токен из памяти.</p><div className="actions"><button className="secondary" onClick={() => void run(async () => { const r = await call<AuthResult>('/api/auth/jwt/me', 'GET', undefined, jwt); setJwtResult(`${r.message}: ${r.username}`) })}>Проверить JWT</button><button className="quiet" onClick={() => { setJwt(''); setJwtResult('JWT удалён из памяти страницы.') }}>Удалить JWT</button></div><output>{jwtResult || (jwt ? 'JWT получен и скрыто хранится в памяти.' : 'Сначала нажми «Войти в демо»')}</output><p className="hint">Для production обычно избегают localStorage для bearer access tokens. Этот упрощённый demo JWT — не полноценная auth-система.</p></section>
    </div>
    <section className="panel mtls"><div><small>03 / mTLS</small><h2>Backend → внутренний ресурс</h2><p>Оба Spring сервиса проверяют сертификат другой стороны. Браузеру клиентский сертификат не нужен.</p></div><button className="secondary" onClick={() => void run(async () => { const r = await call<MtlsResult>('/api/mtls/ping'); setMtlsResult(r.message) })}>Проверить mTLS</button>{mtlsResult && <output>{mtlsResult}</output>}</section>
    {error && <p className="error" role="alert">{error}</p>}
    <section className="note"><h2>Не путай понятия</h2><p><b>Session</b> — состояние на сервере, браузер хранит идентификатор. <b>JWT</b> — подписанные claims, которые клиент предъявляет запросом. <b>mTLS</b> — взаимная аутентификация сервисов в TLS, это не способ входа пользователя.</p><p>Подробности запуска и настройки Render/Cloudflare — в корневом <code>start.txt</code>.</p></section>
    <footer>Учебная демо-среда · Без БД · Не используй демо-секреты в production</footer>
  </main>
}
