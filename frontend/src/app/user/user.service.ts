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
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  readonly me = signal<User | null>(null);

  loadMe() {
    return this.http.get<User>(`${API}/users/me`).pipe(tap((u) => this.me.set(u)));
  }
}
