import { useEffect, useState, type FormEvent } from 'react'
import { login, logout, refresh, setToken as storeToken } from './api'
import Files from './Files'

export default function App() {
  const [token, setToken] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // Restore the session from the httpOnly refresh cookie on page load.
  useEffect(() => {
    refresh().then(setToken).catch(() => {}).finally(() => setLoading(false))
  }, [])

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    try {
      const t = await login(String(form.get('identifier')), String(form.get('password')))
      setToken(t)
      setError('')
    } catch (err) {
      setError((err as Error).message)
    }
  }

  async function onLogout() {
    await logout().catch(() => {})
    setToken(null)
  }

  storeToken(token)

  if (loading) return null

  if (token) {
    return (
      <>
        <h1>LeAIrning</h1>
        <button onClick={onLogout}>Salir</button>
        <Files />
      </>
    )
  }

  return (
    <form onSubmit={onSubmit}>
      <h1>LeAIrning</h1>
      <input name="identifier" placeholder="Usuario o email" required />
      <input name="password" type="password" placeholder="Contraseña" required />
      <button>Entrar</button>
      {error && <p>{error}</p>}
    </form>
  )
}
