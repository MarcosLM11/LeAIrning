const API = 'http://localhost:8080'

export async function login(identifier: string, password: string): Promise<string> {
  const res = await fetch(`${API}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identifier, password }),
  })
  if (!res.ok) throw new Error(`Login failed (${res.status})`)
  const { accessToken } = await res.json()
  return accessToken
}
