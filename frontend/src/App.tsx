import { useEffect, useState, type FormEvent } from 'react'
import { login, logout, refresh } from './api'
import Chat from './Chat'
import Files from './Files'
import Quizz from './Quizz'

export default function App() {
  const [loggedIn, setLoggedIn] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // Restore the session from the httpOnly refresh cookie on page load.
  useEffect(() => {
    refresh().then(() => setLoggedIn(true)).catch(() => {}).finally(() => setLoading(false))
  }, [])

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    try {
      await login(String(form.get('identifier')), String(form.get('password')))
      setLoggedIn(true)
      setError('')
    } catch (err) {
      setError((err as Error).message)
    }
  }

  async function onLogout() {
    await logout().catch(() => {})
    setLoggedIn(false)
  }

  if (loading) return null

  if (loggedIn) {
    return (
      <>
        <h1>LeAIrning</h1>
        <button onClick={onLogout}>Salir</button>
        <Files />
        <Chat />
        <Quizz />
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
