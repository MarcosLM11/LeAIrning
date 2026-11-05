import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API } from '../api';

export type Difficulty = 'FACIL' | 'MEDIO' | 'DIFICIL';
export type QuizzStatus = 'PENDING' | 'READY' | 'FAILED';

export const DIFFICULTIES: { key: Difficulty; label: string }[] = [
  { key: 'FACIL', label: 'Fácil' },
  { key: 'MEDIO', label: 'Medio' },
  { key: 'DIFICIL', label: 'Difícil' },
];

export interface QuizzSummary {
  id: string;
  fileId: string;
  status: QuizzStatus;
  questions: number;
  options: number;
  difficulty: Difficulty;
  createdAt: string;
  score: number | null;
}

export interface Question {
  text: string;
  options: string[];
}

export interface AttemptResult {
  score: number;
  total: number;
  corrections: { correctIndex: number; explanation: string | null }[];
}

export interface QuizzParams {
  fileId: string;
  questions: number;
  options: number;
  difficulty: Difficulty;
}

@Injectable({ providedIn: 'root' })
export class QuizzesService {
  private http = inject(HttpClient);

  list() {
    return this.http.get<QuizzSummary[]>(`${API}/quizz`);
  }

  create({ fileId, ...params }: QuizzParams) {
    return this.http.post<string>(`${API}/quizz/${fileId}`, null, { params });
  }

  get(id: string) {
    return this.http.get<{ questions: Question[] }>(`${API}/quizz/${id}`);
  }

  retry(id: string) {
    return this.http.post<void>(`${API}/quizz/${id}/retry`, null);
  }

  submit(id: string, answers: number[]) {
    return this.http.post<AttemptResult>(`${API}/quizz/${id}/attempts`, { answers });
  }
}
