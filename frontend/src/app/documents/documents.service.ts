import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API } from '../api';

export interface Doc {
  id: string;
  name: string;
  size: number;
  status: 'UPLOADING' | 'UPLOADED' | 'FAILED';
  uploadedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private http = inject(HttpClient);

  list() {
    return this.http.get<Doc[]>(`${API}/files`);
  }

  upload(file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.post(`${API}/files`, body, { responseType: 'text' });
  }
}
