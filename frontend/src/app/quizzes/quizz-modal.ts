import { Component, ElementRef, afterNextRender, inject, input, output, signal, viewChild } from '@angular/core';
import { DocumentsService } from '../documents/documents.service';
import { DIFFICULTIES, Difficulty, QuizzParams } from './quizzes.service';

const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';

// <dialog> nativo: pinta el fondo, atrapa el foco y cierra con Esc.
@Component({
  selector: 'app-quizz-modal',
  template: `
    <dialog #dlg (close)="closed.emit()" (click)="$event.target === dlg && dlg.close()"
            class="m-auto w-full max-w-[480px] rounded-2xl border border-line bg-card p-0 text-ink backdrop:bg-black/40">
      <div class="flex items-start justify-between p-6 pb-4">
        <div>
          <h2 class="text-base font-semibold tracking-tight">Nuevo cuestionario</h2>
          <p class="mt-0.5 text-[13px] text-mute">Elige un documento y cómo quieres que sea</p>
        </div>
        <button (click)="dlg.close()" aria-label="Cerrar" class="-mr-2 -mt-1 grid size-8 place-items-center rounded-lg text-mute hover:bg-app hover:text-ink">
          <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
        </button>
      </div>

      <div class="space-y-4 px-6 pb-2">
        <label class="block text-[13px] font-medium">
          Documento
          <select class="input select mt-1.5 font-normal" (change)="fileId.set($any($event.target).value)">
            @for (d of docs(); track d.id) { <option [value]="d.id" [selected]="d.id === fileId()">{{ d.name }}</option> }
          </select>
        </label>

        <div class="grid grid-cols-2 gap-4">
          @for (f of steppers; track f.label) {
            <div class="text-[13px] font-medium">
              {{ f.label }}
              <div class="mt-1.5 flex h-10 items-center justify-between rounded-[10px] border border-line bg-card px-3 text-sm font-normal">
                {{ f.value() }}
                <span class="flex gap-1 text-mute">
                  <button (click)="f.value.set(Math.max(f.min, f.value() - 1))" aria-label="Menos" class="grid size-6 place-items-center rounded hover:text-ink">
                    <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M5 12h14" /></svg>
                  </button>
                  <button (click)="f.value.set(Math.min(f.max, f.value() + 1))" aria-label="Más" class="grid size-6 place-items-center rounded hover:text-ink">
                    <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
                  </button>
                </span>
              </div>
            </div>
          }
        </div>

        <div class="text-[13px] font-medium">
          Dificultad
          <div class="mt-1.5 flex h-10 rounded-[10px] border border-line bg-app p-1 text-[13px]">
            @for (d of difficulties; track d) {
              <button (click)="difficulty.set(d.key)" [class]="d.key === difficulty() ? 'bg-card shadow-sm' : 'text-mute'" class="flex-1 rounded-md">{{ d.label }}</button>
            }
          </div>
        </div>

        <p class="flex items-center gap-1.5 rounded-[10px] bg-app px-3 py-2.5 text-[13px] text-mute">
          <svg class="size-3.5 shrink-0 ${ICON}" viewBox="0 0 24 24"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-10 5L2 7" /></svg>
          Tardará un poco. Te avisaremos por email cuando esté listo.
        </p>
      </div>

      <div class="flex justify-end gap-2 p-6 pt-5">
        <button (click)="dlg.close()" class="h-10 rounded-[10px] border border-line px-4 text-sm font-medium hover:bg-app">Cancelar</button>
        <button (click)="submit()" [disabled]="!fileId()" class="flex h-10 items-center gap-2 rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover disabled:opacity-50">
          <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="m12 3 1.9 5.8L20 10l-6.1 1.2L12 17l-1.9-5.8L4 10l6.1-1.2z" /></svg>
          Generar
        </button>
      </div>
    </dialog>
  `,
})

export class QuizzModal {
  Math = Math;
  closed = output();
  generate = output<QuizzParams>();
  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  docs = signal<{ id: string; name: string }[]>([]);
  difficulties = DIFFICULTIES;
  preselected = input('');
  fileId = signal('');
  difficulty = signal<Difficulty>('MEDIO');
  questions = signal(10);
  options = signal(4);
  steppers = [
    { label: 'Preguntas', value: this.questions, min: 1, max: 30 },
    { label: 'Opciones por pregunta', value: this.options, min: 2, max: 6 },
  ];

  constructor() {
    inject(DocumentsService).list().subscribe((l) => {
      this.docs.set(l.filter((d) => d.status === 'READY'));
      this.fileId.set(this.docs().find((d) => d.id === this.preselected())?.id ?? this.docs()[0]?.id ?? '');
    });
    afterNextRender(() => this.dlg().nativeElement.showModal());
  }

  submit() {
    this.generate.emit({ fileId: this.fileId(), questions: this.questions(), options: this.options(), difficulty: this.difficulty() });
    this.dlg().nativeElement.close();
  }
}
