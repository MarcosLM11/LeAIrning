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
