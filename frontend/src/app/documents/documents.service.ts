import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { API } from '../api';

export interface Doc {
  id: string;
  name: string;
  contentType: string;
  size: number;
  status: 'UPLOADING' | 'UPLOADED' | 'READY' | 'FAILED';
  uploadedAt: string;
}

export interface DocPage {
  content: Doc[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface DocQuery {
  page: number;
  size: number;
  name?: string;
  type?: string;
  since?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private http = inject(HttpClient);

  page(q: DocQuery) {
    const params: Record<string, string | number> = { page: q.page, size: q.size };
    if (q.name) params['name'] = q.name;
    if (q.type) params['type'] = q.type;
    if (q.since) params['since'] = q.since;
    return this.http.get<DocPage>(`${API}/files`, { params });
  }

  // shortcut: loads only the newest 100 files (backend max page size); add paging UI if users exceed that.
  list() {
    return this.page({ page: 0, size: 100 }).pipe(map((p) => p.content));
  }

  remove(id: string) {
    return this.http.delete<void>(`${API}/files/${id}`);
  }

  upload(file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.post(`${API}/files`, body, { responseType: 'text' });
  }
}
