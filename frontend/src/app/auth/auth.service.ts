import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API } from '../api';


@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  readonly token = signal<string | null>(null);

  login(identifier: string, password: string) {
    return this.http
      .post<{ accessToken: string }>(`${API}/auth/login`, { identifier, password }, { withCredentials: true })
      .pipe(tap((r) => this.token.set(r.accessToken)));
  }

  refresh() {
    return this.http
      .post<{ accessToken: string }>(`${API}/auth/refresh`, null, { withCredentials: true })
      .pipe(tap((r) => this.token.set(r.accessToken)));
  }

  register(user: { name: string; username: string; email: string; password: string }) {
    return this.http.post(`${API}/users`, user);
  }
}
