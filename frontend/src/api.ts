const API = 'http://localhost:8080'

// The access token lives only in memory; the refresh token is an httpOnly cookie.
let token: string | null = null

async function auth(path: string, body?: object): Promise<void> {
  const res = await fetch(`${API}/api/v1/auth/${path}`, {
    method: 'POST',
    credentials: 'include', // sends/stores the httpOnly refresh cookie
    headers: { 'Content-Type': 'application/json' },
    body: body && JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${path} failed (${res.status})`)
  token = res.status === 204 ? null : (await res.json()).accessToken
}

export const login = (identifier: string, password: string) => auth('login', { identifier, password })
export const logout = () => auth('logout')

// One refresh at a time: refresh tokens rotate, so parallel calls (parallel 401s, StrictMode) must share it.
let refreshing: Promise<void> | null = null
export const refresh = () => (refreshing ??= auth('refresh').finally(() => { refreshing = null }))

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const send = () => fetch(`${API}${path}`, { ...init, headers: { ...init.headers, Authorization: `Bearer ${token}` } })
  let res = await send()
  if (res.status === 401) {
    await refresh()
    res = await send()
  }
  if (!res.ok) throw new Error(`Request failed (${res.status})`)
  return res
}

export type FileItem = {
  id: string
  name: string
  contentType: string
  size: number
  status: 'UPLOADING' | 'UPLOADED' | 'FAILED'
  uploadedAt: string
}

export const listFiles = async (): Promise<FileItem[]> => (await request('/api/v1/files')).json()
export const deleteFile = (id: string) => request(`/api/v1/files/${id}`, { method: 'DELETE' })
export const uploadFile = (file: File) => {
  const body = new FormData()
  body.append('file', file)
  return request('/api/v1/files', { method: 'POST', body })
}

export const ask = async (question: string): Promise<string> =>
  (await request('/chat', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: question })).text()

export type QuizzView = {
  id: string
  status: 'PENDING' | 'READY' | 'FAILED'
  questions: { text: string; options: string[] }[]
}

export const startQuizz = async (fileId: string): Promise<string> =>
  (await request(`/quizz/${fileId}`, { method: 'POST' })).json()
export const getQuizz = async (id: string): Promise<QuizzView> => (await request(`/quizz/${id}`)).json()
