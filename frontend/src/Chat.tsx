import { useState, type FormEvent } from 'react'
import { ask } from './api'

type Message = { from: 'me' | 'ai'; text: string }

export default function Chat() {
  const [messages, setMessages] = useState<Message[]>([])
  const [pending, setPending] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const question = String(new FormData(form).get('question'))
    form.reset()
    setMessages(m => [...m, { from: 'me', text: question }])
    setPending(true)
    try {
      const answer = await ask(question)
      setMessages(m => [...m, { from: 'ai', text: answer }])
    } catch (err) {
      setMessages(m => [...m, { from: 'ai', text: (err as Error).message }])
    } finally {
      setPending(false)
    }
  }

  return (
    <>
      <h2>Chat</h2>
      {messages.map((m, i) => <p key={i}><b>{m.from}:</b> {m.text}</p>)}
      <form onSubmit={onSubmit}>
        <input name="question" required />
        <button disabled={pending}>Enviar</button>
      </form>
    </>
  )
}
