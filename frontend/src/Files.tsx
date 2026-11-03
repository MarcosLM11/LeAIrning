import { useCallback, useEffect, useState, type ChangeEvent } from 'react'
import { deleteFile, listFiles, uploadFile, type FileItem } from './api'

export default function Files() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [error, setError] = useState('')

  const reload = useCallback(() => listFiles().then(setFiles).catch((e: Error) => setError(e.message)), [])

  useEffect(() => { reload() }, [reload])

  async function onUpload(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      await uploadFile(file)
      setError('')
    } catch (err) {
      setError((err as Error).message)
    }
    e.target.value = ''
    reload()
  }

  async function onDelete(id: string) {
    await deleteFile(id).catch((e: Error) => setError(e.message))
    reload()
  }

  return (
    <>
      <input type="file" onChange={onUpload} />
      {error && <p>{error}</p>}
      <ul>
        {files.map(f => (
          <li key={f.id}>
            {f.name} — {f.size} B — {f.status}{' '}
            <button onClick={() => onDelete(f.id)}>Borrar</button>
          </li>
        ))}
      </ul>
    </>
  )
}
