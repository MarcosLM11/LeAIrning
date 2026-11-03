import { useState, type FormEvent } from 'react'
import { login } from './api'

export default function App() {
  const [token, setToken] = useState<string | null>(null)
  const [error, setError] = useState('')

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

  function logout() {
    setToken(null)
  }

  if (token) {
    return (
      <>
        <h1>LeAIrning</h1>
        <p>Sesión iniciada.</p>
        <button onClick={logout}>Salir</button>
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
