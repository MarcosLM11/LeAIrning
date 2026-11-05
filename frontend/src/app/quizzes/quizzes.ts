import { Component, signal } from '@angular/core';
import { QuizzModal, QuizzParams } from './quizz-modal';
import { QuizzPlay } from './quizz-play';

type Status = 'PENDING' | 'READY' | 'FAILED';
type Quizz = { doc: string; questions: number; options: number; difficulty: string; when: string; status: Status; score?: number };

const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';
const BADGE: Record<Status, { label: string; cls: string }> = {
  PENDING: { label: 'Generando…', cls: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300' },
  READY: { label: 'Listo', cls: 'bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300' },
  FAILED: { label: 'Error', cls: 'bg-red-100 text-red-800 dark:bg-red-500/15 dark:text-red-300' },
};

// Boceto: datos mock, sin backend todavía.
@Component({
  selector: 'app-quizzes',
  imports: [QuizzModal, QuizzPlay],
  host: { class: 'mx-auto block max-w-[960px]' },
  template: `
    <header class="mb-8 flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Cuestionarios</h1>
        <p class="mt-1 text-sm text-mute">Genera preguntas a partir de tus documentos y ponte a prueba</p>
      </div>
      <button (click)="modal.set(true)" class="flex h-10 items-center gap-2 rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover">
        <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
        Nuevo cuestionario
      </button>
    </header>

    <h2 class="mb-3 text-sm font-semibold">Tus cuestionarios</h2>
    <div class="divide-y divide-line overflow-hidden rounded-2xl border border-line bg-card">
      @for (q of quizzes(); track q) {
        <div class="flex items-center gap-4 px-5 py-4">
          <span class="grid size-10 shrink-0 place-items-center rounded-[10px] bg-app text-mute">
            <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /></svg>
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium">{{ q.doc }}</p>
            <p class="text-[13px] text-mute">{{ q.questions }} preguntas · {{ q.options }} opciones · {{ q.difficulty }} · {{ q.when }}</p>
          </div>
          @if (q.score !== undefined) {
            <span class="text-sm font-semibold">{{ q.score }}<span class="font-normal text-mute"> / {{ q.questions }}</span></span>
          } @else {
            <span [class]="badge[q.status].cls" class="rounded-full px-2.5 py-1 text-xs font-medium">{{ badge[q.status].label }}</span>
          }
          @if (q.status !== 'PENDING') {
            <button (click)="q.status === 'READY' && play.set(q)" class="flex h-9 items-center gap-1.5 rounded-[10px] border border-line px-3 text-sm font-medium hover:bg-app">
              {{ q.status === 'FAILED' ? 'Reintentar' : q.score !== undefined ? 'Repetir' : 'Empezar' }}
            </button>
          }
        </div>
      }
    </div>

    @if (play(); as q) {
      <app-quizz-play [doc]="q.doc" [difficulty]="q.difficulty" (closed)="play.set(null)" (finished)="finish(q, $event.score)" />
    }
    @if (modal()) { <app-quizz-modal (closed)="modal.set(false)" (generate)="generate($event)" /> }
  `,
})

export class Quizzes {
  badge = BADGE;
  modal = signal(false);
  play = signal<Quizz | null>(null);
  quizzes = signal<Quizz[]>([
    { doc: 'apuntes-kafka.docx', questions: 10, options: 4, difficulty: 'Medio', when: 'hace 1 min', status: 'PENDING' },
    { doc: 'arquitectura-mensajeria.pdf', questions: 10, options: 4, difficulty: 'Medio', when: 'hoy', status: 'READY' },
    { doc: 'patrones-diseno.pdf', questions: 15, options: 4, difficulty: 'Difícil', when: 'ayer', status: 'READY', score: 12 },
    { doc: 'spring-security.docx', questions: 10, options: 3, difficulty: 'Fácil', when: 'hace 3 días', status: 'FAILED' },
  ]);

  // Mock: añade un cuestionario "Generando…" sin llamar al backend.
  generate(p: QuizzParams) {
    this.quizzes.update((l) => [{ ...p, when: 'ahora', status: 'PENDING' }, ...l]);
  }

  // Mock: guarda la nota en la lista (el backend aún no la persiste).
  finish(q: Quizz, score: number) {
    const n = { ...q, score };
    this.quizzes.update((l) => l.map((x) => (x === q ? n : x)));
    this.play.set(n);
  }
}
