import { Component, ElementRef, OnInit, afterNextRender, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { AttemptResult, Question, QuizzesService } from './quizzes.service';

const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';
const LETTERS = 'ABCDEF';
// <dialog> nativo: pinta el fondo, atrapa el foco y cierra con Esc.
@Component({
  selector: 'app-quizz-play',
  template: `
    <dialog #dlg (close)="closed.emit()" (click)="$event.target === dlg && dlg.close()"
            class="m-auto w-full max-w-[560px] overflow-hidden rounded-2xl border border-line bg-card p-0 text-ink backdrop:bg-black/40">
      <div class="flex max-h-[640px] flex-col">
        @if (error()) {
          <p class="p-6 text-sm text-red-600">{{ error() }}</p>
        } @else if (!questions().length) {
          <p class="p-6 text-sm text-mute">Cargando…</p>
        } @else if (!done()) {
          <div class="p-6 pb-4">
            <div class="flex items-start justify-between">
              <div class="min-w-0">
                <p class="truncate text-[13px] text-mute">{{ doc() }} · {{ difficulty() }}</p>
                <h2 class="mt-0.5 text-sm font-semibold">Pregunta {{ i() + 1 }} de {{ total }}</h2>
              </div>
              <button (click)="dlg.close()" aria-label="Cerrar" class="-mr-2 -mt-1 grid size-8 place-items-center rounded-lg text-mute hover:bg-app hover:text-ink">
                <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
              </button>
            </div>
            <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-app">
              <div class="h-full rounded-full bg-brand transition-[width]" [style.width.%]="((i() + 1) / total) * 100"></div>
            </div>
          </div>
          <div class="overflow-y-auto px-6 pb-2">
            <p class="mb-4 text-[17px] font-semibold leading-snug tracking-tight">{{ q().text }}</p>
            <div class="space-y-2.5">
              @for (o of q().options; track $index) {
                <button (click)="pick($index)"
                        [class]="answers()[i()] === $index ? 'border-2 border-brand bg-brand/10' : 'border border-line hover:bg-app'"
                        class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm">
                  <span [class]="answers()[i()] === $index ? 'bg-brand text-white' : 'border border-line text-mute'"
                        class="grid size-6 shrink-0 place-items-center rounded-full text-xs font-medium">{{ letters[$index] }}</span>
                  <span class="flex-1">{{ o }}</span>
                  @if (answers()[i()] === $index) {
                    <svg class="size-4 text-brand ${ICON}" viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5" /></svg>
                  }
                </button>
              }
            </div>
          </div>
          <div class="flex items-center justify-between p-6 pt-5">
            <button (click)="i.set(i() - 1)" [class.invisible]="i() === 0" class="flex h-10 items-center gap-1.5 rounded-[10px] px-3 text-sm font-medium text-mute hover:bg-app">
              <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M19 12H5M12 19l-7-7 7-7" /></svg>Anterior
            </button>
            <button (click)="next()" [disabled]="answers()[i()] === undefined"
                    class="flex h-10 items-center gap-2 rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover disabled:opacity-50">
              {{ i() === total - 1 ? 'Finalizar' : 'Siguiente' }}
              <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
            </button>
          </div>
        } @else {
          <div class="flex items-start justify-between p-6 pb-2">
            <p class="truncate text-[13px] text-mute">{{ doc() }} · {{ difficulty() }}</p>
            <button (click)="dlg.close()" aria-label="Cerrar" class="-mr-2 -mt-1 grid size-8 place-items-center rounded-lg text-mute hover:bg-app hover:text-ink">
              <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
            </button>
          </div>
          <div class="flex flex-col items-center px-6 pb-5 text-center">
            <div class="relative grid size-28 place-items-center">
              <svg viewBox="0 0 100 100" class="absolute inset-0 size-full -rotate-90">
                <circle cx="50" cy="50" r="44" fill="none" stroke-width="8" class="stroke-app" />
                <circle cx="50" cy="50" r="44" fill="none" stroke-width="8" stroke-linecap="round" class="stroke-brand"
                        stroke-dasharray="276.5" [attr.stroke-dashoffset]="276.5 * (1 - score() / total)" />
              </svg>
              <p class="text-3xl font-semibold tracking-tight">{{ score() }}<span class="text-lg font-normal text-mute"> / {{ total }}</span></p>
            </div>
            <h2 class="mt-4 text-lg font-semibold tracking-tight">{{ message() }}</h2>
            <p class="mt-0.5 text-sm text-mute">Has acertado el {{ ((score() / total) * 100).toFixed(0) }} % de las preguntas</p>
            <div class="mt-4 flex gap-2 text-[13px] font-medium">
              <span class="rounded-full bg-green-100 px-3 py-1 text-green-800 dark:bg-green-500/15 dark:text-green-300">{{ score() }} aciertos</span>
              <span class="rounded-full bg-red-100 px-3 py-1 text-red-800 dark:bg-red-500/15 dark:text-red-300">{{ total - score() }} fallos</span>
            </div>
          </div>
          <div class="min-h-0 flex-1 overflow-y-auto border-t border-line">
            <p class="px-6 pb-2 pt-4 text-sm font-semibold">Revisión</p>
            <div class="mx-6 mb-4 divide-y divide-line overflow-hidden rounded-xl border border-line">
              @for (q of questions(); track $index) {
                <div class="px-4 py-3">
                  <button (click)="toggle($index)" class="flex w-full items-center gap-3 text-left">
                    <span [class]="right($index) ? 'bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300' : 'bg-red-100 text-red-800 dark:bg-red-500/15 dark:text-red-300'"
                          class="grid size-6 shrink-0 place-items-center rounded-full">
                      <svg class="size-3.5 ${ICON}" viewBox="0 0 24 24">
                        @if (right($index)) { <path d="M20 6 9 17l-5-5" /> } @else { <path d="M18 6 6 18M6 6l12 12" /> }
                      </svg>
                    </span>
                    <p class="flex-1 text-sm"><span class="text-mute">{{ $index + 1 }}.</span> {{ q.text }}</p>
                    <svg class="size-4 text-mute ${ICON}" viewBox="0 0 24 24"><path [attr.d]="open().has($index) ? 'm18 15-6-6-6 6' : 'm6 9 6 6 6-6'" /></svg>
                  </button>
                  @if (open().has($index)) {
                    <div class="ml-9 mt-3 space-y-2 text-[13px]">
                      @if (!right($index)) {
                        <p class="text-red-700 dark:text-red-300"><span class="font-medium">Tu respuesta: </span>{{ q.options[answers()[$index]] }}</p>
                      }
                      <p class="text-green-700 dark:text-green-300"><span class="font-medium">Correcta: </span>{{ q.options[corrections()[$index].correctIndex] }}</p>
                      @if (corrections()[$index].explanation; as e) {
                        <p class="rounded-[10px] bg-app px-3 py-2 text-mute">{{ e }}</p>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </div>
          <div class="flex items-center justify-between border-t border-line p-6 pt-5">
            <button (click)="dlg.close()" class="h-10 rounded-[10px] px-3 text-sm font-medium text-mute hover:bg-app">Cerrar</button>
            <button (click)="restart()" class="flex h-10 items-center gap-2 rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover">
              <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5" /></svg>
              Repetir cuestionario
            </button>
          </div>
        }
      </div>
    </dialog>
  `,
})

export class QuizzPlay implements OnInit {
  id = input.required<string>();
  doc = input.required<string>();
  difficulty = input.required<string>();
  closed = output();
  finished = output();
  private service = inject(QuizzesService);
  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  questions = signal<Question[]>([]);
  result = signal<AttemptResult | null>(null);
  error = signal('');
  letters = LETTERS;
  i = signal(0);
  answers = signal<number[]>([]);
  open = signal(new Set<number>());

  get total() {
    return this.questions().length;
  }
  done = computed(() => this.result() !== null);
  corrections = computed(() => this.result()?.corrections ?? []);
  q = computed(() => this.questions()[this.i()]);
  score = computed(() => this.result()?.score ?? 0);
  message = computed(() => {
    const r = this.score() / this.total;
    return r >= 0.8 ? '¡Muy bien!' : r >= 0.5 ? 'Buen intento' : 'Sigue practicando';
  });

  constructor() {
    afterNextRender(() => this.dlg().nativeElement.showModal());
  }

  ngOnInit() {
    this.service.get(this.id()).subscribe({
      next: (q) => this.questions.set(q.questions),
      error: () => this.error.set('No se pudo cargar el cuestionario'),
    });
  }

  right(n: number) {
    return this.answers()[n] === this.corrections()[n]?.correctIndex;
  }

  pick(o: number) {
    this.answers.update((a) => Object.assign([...a], { [this.i()]: o }));
  }

  next() {
    if (this.i() < this.total - 1) return this.i.update((n) => n + 1);
    this.service.submit(this.id(), this.answers()).subscribe({
      next: (r) => {
        this.result.set(r);
        this.finished.emit();
      },
      error: () => this.error.set('No se pudo enviar el cuestionario'),
    });
  }

  toggle(n: number) {
    this.open.update((s) => {
      const c = new Set(s);
      c.delete(n) || c.add(n);
      return c;
    });
  }

  restart() {
    this.i.set(0);
    this.answers.set([]);
    this.open.set(new Set());
    this.result.set(null);
  }
}
