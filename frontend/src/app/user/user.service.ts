import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API } from '../api';

export interface User {
  id: string;
  email: string;
  username: string;
  name: string;
  createdAt: string;
  hasAvatar: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  readonly me = signal<User | null>(null);
  readonly avatarUrl = signal<string | null>(null);

  update(name: string, username: string) {
    return this.http.put<User>(`${API}/users/${this.me()!.id}`, { name, username }).pipe(tap((u) => this.me.set(u)));
  }

  loadMe() {
    return this.http.get<User>(`${API}/users/me`).pipe(tap((u) => { this.me.set(u); this.loadAvatar(u); }));
  }

  uploadAvatar(file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.put<User>(`${API}/users/${this.me()!.id}/avatar`, body).pipe(tap((u) => { this.me.set(u); this.loadAvatar(u); }));
  }

  private loadAvatar(u: User) {
    if (!u.hasAvatar) return this.avatarUrl.set(null);
    this.http.get(`${API}/users/${u.id}/avatar`, { responseType: 'blob' })
      .subscribe((b) => this.avatarUrl.set(URL.createObjectURL(b)));
  }
}
