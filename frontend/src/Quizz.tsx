import { useEffect, useState, type FormEvent } from 'react'
import { getQuizz, listFiles, startQuizz, type FileItem, type QuizzView } from './api'

export default function Quizz() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [quizzId, setQuizzId] = useState<string | null>(null)
  const [quizz, setQuizz] = useState<QuizzView | null>(null)
  const [error, setError] = useState('')

  useEffect(() => { listFiles().then(setFiles).catch((e: Error) => setError(e.message)) }, [])

  // Poll until the async generation finishes (READY or FAILED).
  useEffect(() => {
    if (!quizzId) return
    const timer = setInterval(async () => {
      try {
        const q = await getQuizz(quizzId)
        setQuizz(q)
        if (q.status !== 'PENDING') clearInterval(timer)
      } catch (err) {
        setError((err as Error).message)
        clearInterval(timer)
      }
    }, 2000)
    return () => clearInterval(timer)
  }, [quizzId])

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setQuizz(null)
    setError('')
    try {
      setQuizzId(await startQuizz(String(new FormData(e.currentTarget).get('fileId'))))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <>
      <h2>Quizz</h2>
      <form onSubmit={onSubmit}>
        <select name="fileId" required>
          {files.filter(f => f.status === 'UPLOADED').map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
        </select>
        <button>Generar</button>
      </form>
      {error && <p>{error}</p>}
      {quizzId && !quizz && <p>Generando…</p>}
      {quizz && <p>Estado: {quizz.status}</p>}
      {quizz?.questions.map((q, i) => (
        <div key={i}>
          <p><b>{i + 1}. {q.text}</b></p>
          <ul>{q.options.map(o => <li key={o}>{o}</li>)}</ul>
        </div>
      ))}
    </>
  )
}
