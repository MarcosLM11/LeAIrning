import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API } from '../api';
import { Page } from '../page';

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

  list(page: number, size: number) {
    return this.http.get<Page<QuizzSummary>>(`${API}/quizzes`, { params: { page, size } });
  }

  create({ fileId, ...params }: QuizzParams) {
    return this.http.post<string>(`${API}/quizzes/${fileId}`, null, { params });
  }

  get(id: string) {
    return this.http.get<{ questions: Question[] }>(`${API}/quizzes/${id}`);
  }

  retry(id: string) {
    return this.http.post<void>(`${API}/quizzes/${id}/retry`, null);
  }

  submit(id: string, answers: number[]) {
    return this.http.post<AttemptResult>(`${API}/quizzes/${id}/attempts`, { answers });
  }
}
