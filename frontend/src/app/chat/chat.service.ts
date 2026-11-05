import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../api';
import { AuthService } from '../auth/auth.service';

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
}

export interface Message {
  role: 'user' | 'assistant';
  text: string;
}

// An SSE event may carry several "data:" lines; the server writes "data: " + chunk, so one leading space is separator.
const dataOf = (event: string) =>
  event.split('\n').filter((l) => l.startsWith('data:')).map((l) => l.slice(5).replace(/^ /, '')).join('\n');

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  conversations() {
    return this.http.get<Conversation[]>(`${API}/chat/conversations`);
  }

  history(conversationId: string) {
    return this.http.get<Message[]>(`${API}/chat/history`, { params: new HttpParams({ fromObject: { conversationId } }) });
  }

  remove(conversationId: string) {
    return this.http.delete<void>(`${API}/chat/history`, { params: new HttpParams({ fromObject: { conversationId } }) });
  }

  // fetch instead of HttpClient/EventSource: the endpoint is a POST with an Authorization header and a streamed body.
  stream(question: string, conversationId: string, fileIds: string[]) {
    return new Observable<string>((sub) => {
      const ctrl = new AbortController();
      const params = new URLSearchParams({ conversationId });
      if (fileIds.length) params.set('fileIds', fileIds.join(','));
      fetch(`${API}/chat/stream?${params}`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain', Authorization: `Bearer ${this.auth.token()}` },
        body: question,
        signal: ctrl.signal,
      })
        .then(async (res) => {
          if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`);
          const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
          let buffer = '';
          for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            const events = (buffer + value).split('\n\n');
            buffer = events.pop()!;
            events.forEach((e) => sub.next(dataOf(e)));
          }
          sub.complete();
        })
        .catch((e) => sub.error(e));
      return () => ctrl.abort();
    });
  }
}
