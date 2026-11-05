import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, tap } from 'rxjs';
import { API } from '../api';
import { UserService } from '../user/user.service';


@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private users = inject(UserService);
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

  logout() {
    this.http.post(`${API}/auth/logout`, null, { withCredentials: true })
      .pipe(finalize(() => {
        this.token.set(null);
        this.users.clear();
        this.router.navigateByUrl('/login');
      }))
      .subscribe({ error: () => {} });
  }

  register(user: { name: string; username: string; email: string; password: string }) {
    return this.http.post(`${API}/users`, user);
  }
}
