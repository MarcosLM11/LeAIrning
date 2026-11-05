import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { fmtDate } from '../documents/doc-format';
import { DocumentsService } from '../documents/documents.service';
import { QuizzModal } from './quizz-modal';
import { QuizzPlay } from './quizz-play';
import { DIFFICULTIES, QuizzParams, QuizzStatus, QuizzSummary, QuizzesService } from './quizzes.service';

const LABEL = Object.fromEntries(DIFFICULTIES.map((d) => [d.key, d.label]));

const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';
const BADGE: Record<QuizzStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Generando…', cls: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300' },
  READY: { label: 'Listo', cls: 'bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300' },
  FAILED: { label: 'Error', cls: 'bg-red-100 text-red-800 dark:bg-red-500/15 dark:text-red-300' },
};

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

    @if (error()) { <p class="mb-3 text-sm text-red-600">{{ error() }}</p> }
    <h2 class="mb-3 text-sm font-semibold">Tus cuestionarios</h2>
    <div class="divide-y divide-line overflow-hidden rounded-2xl border border-line bg-card">
      @for (q of quizzes(); track q.id) {
        <div class="flex items-center gap-4 px-5 py-4">
          <span class="grid size-10 shrink-0 place-items-center rounded-[10px] bg-app text-mute">
            <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /></svg>
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium">{{ name(q) }}</p>
            <p class="text-[13px] text-mute">{{ q.questions }} preguntas · {{ q.options }} opciones · {{ label[q.difficulty] }} · {{ fmtDate(q.createdAt) }}</p>
          </div>
          @if (q.score !== null) {
            <span class="text-sm font-semibold">{{ q.score }}<span class="font-normal text-mute"> / {{ q.questions }}</span></span>
          } @else {
            <span [class]="badge[q.status].cls" class="rounded-full px-2.5 py-1 text-xs font-medium">{{ badge[q.status].label }}</span>
          }
          @if (q.status === 'READY') {
            <button (click)="play.set(q)" class="flex h-9 items-center gap-1.5 rounded-[10px] border border-line px-3 text-sm font-medium hover:bg-app">
              {{ q.score !== null ? 'Repetir' : 'Empezar' }}
            </button>
          } @else if (q.status === 'FAILED') {
            <button (click)="retry(q)" class="flex h-9 items-center gap-1.5 rounded-[10px] border border-line px-3 text-sm font-medium hover:bg-app">Reintentar</button>
          }
        </div>
      }
    </div>

    @if (play(); as q) {
      <app-quizz-play [id]="q.id" [doc]="name(q)" [difficulty]="label[q.difficulty]" (closed)="play.set(null)" (finished)="load()" />
    }
    @if (modal()) { <app-quizz-modal [preselected]="doc" (closed)="modal.set(false); doc = ''" (generate)="generate($event)" /> }
  `,
})

export class Quizzes {
  private service = inject(QuizzesService);
  private timer?: ReturnType<typeof setTimeout>;
  badge = BADGE;
  label = LABEL;
  fmtDate = fmtDate;
  doc = inject(ActivatedRoute).snapshot.queryParamMap.get('doc') ?? '';
  modal = signal(!!this.doc);
  error = signal('');
  play = signal<QuizzSummary | null>(null);
  quizzes = signal<QuizzSummary[]>([]);
  private files = signal(new Map<string, string>());

  constructor() {
    inject(DocumentsService).list().subscribe((l) => this.files.set(new Map(l.map((d) => [d.id, d.name]))));
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
    this.load();
  }

  name(q: QuizzSummary) {
    return this.files().get(q.fileId) ?? '—';
  }

  // Reloads the list; while something is still generating it polls every 5 s.
  load() {
    clearTimeout(this.timer);
    this.service.list().subscribe((l) => {
      this.quizzes.set(l);
      if (l.some((q) => q.status === 'PENDING')) this.timer = setTimeout(() => this.load(), 5000);
    });
  }

  retry(q: QuizzSummary) {
    this.error.set('');
    this.service.retry(q.id).subscribe({ next: () => this.load(), error: () => this.error.set('No se pudo reintentar el cuestionario') });
  }

  generate(p: QuizzParams) {
    this.error.set('');
    this.service.create(p).subscribe({ next: () => this.load(), error: () => this.error.set('No se pudo crear el cuestionario') });
  }
}
