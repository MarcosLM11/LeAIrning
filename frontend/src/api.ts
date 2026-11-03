const API = 'http://localhost:8080'

async function auth(path: string, body?: object): Promise<string | null> {
  const res = await fetch(`${API}/api/v1/auth/${path}`, {
    method: 'POST',
    credentials: 'include', // sends/stores the httpOnly refresh cookie
    headers: { 'Content-Type': 'application/json' },
    body: body && JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${path} failed (${res.status})`)
  return res.status === 204 ? null : (await res.json()).accessToken
}

export const login = (identifier: string, password: string) =>
  auth('login', { identifier, password }) as Promise<string>
export const refresh = () => auth('refresh') as Promise<string>
export const logout = () => auth('logout')

let token: string | null = null
export const setToken = (t: string | null) => { token = t }

export type FileItem = {
  id: string
  name: string
  contentType: string
  size: number
  status: 'UPLOADING' | 'UPLOADED' | 'FAILED'
  uploadedAt: string
}

async function request(path: string, init: RequestInit = {}) {
  const res = await fetch(`${API}/api/v1/files${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error(`Request failed (${res.status})`)
  return res
}

export const listFiles = async (): Promise<FileItem[]> => (await request('')).json()
export const deleteFile = (id: string) => request(`/${id}`, { method: 'DELETE' })
export const uploadFile = (file: File) => {
  const body = new FormData()
  body.append('file', file)
  return request('', { method: 'POST', body })
}
