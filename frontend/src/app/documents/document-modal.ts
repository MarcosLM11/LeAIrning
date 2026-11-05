import { Component, ElementRef, afterNextRender, computed, input, output, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fmtDate, fmtSize, STATUS } from './doc-format';
import { Doc } from './documents.service';
import { FileIcon } from './file-icon';
import { typeOf } from './file-type';

// <dialog> nativo: pinta el fondo, atrapa el foco y cierra con Esc.
@Component({
  selector: 'app-document-modal',
  imports: [RouterLink, FileIcon],
  template: `
    <dialog #dlg (close)="closed.emit()" (click)="$event.target === dlg && dlg.close()"
            class="m-auto w-full max-w-[480px] rounded-2xl border border-line bg-card p-0 text-ink backdrop:bg-black/40">
      <div class="flex items-start gap-4 p-6 pb-5">
        <app-file-icon [name]="doc().name" />
        <div class="min-w-0 flex-1 pt-0.5">
          <h2 class="break-words text-base font-semibold tracking-tight">{{ doc().name }}</h2>
          <p class="mt-0.5 text-[13px] text-mute">{{ type().label }}</p>
        </div>
        <button (click)="dlg.close()" aria-label="Cerrar" class="-mr-2 -mt-1 grid size-8 place-items-center rounded-lg text-mute hover:bg-app hover:text-ink">
          <svg class="size-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
        </button>
      </div>
      <dl class="mx-6 divide-y divide-line border-t border-line text-sm">
        @for (r of rows(); track r[0]) {
          <div class="flex items-center justify-between gap-6 py-3">
            <dt class="shrink-0 text-mute">{{ r[0] }}</dt>
            <dd class="break-all text-right font-medium">{{ r[1] }}</dd>
          </div>
        }
      </dl>
      <div class="flex items-center justify-between gap-3 p-6 pt-5">
        <button (click)="remove.emit()" class="h-10 rounded-[10px] px-3 text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950">Eliminar</button>
        <div class="flex gap-2">
          <a routerLink="/quizzes" class="grid h-10 place-items-center rounded-[10px] border border-line px-4 text-sm font-medium hover:bg-app">Cuestionario</a>
          <a routerLink="/chat" [queryParams]="{ doc: doc().id }" class="grid h-10 place-items-center rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover">Chat</a>
        </div>
      </div>
    </dialog>
  `,
})

export class DocumentModal {
  doc = input.required<Doc>();
  closed = output();
  remove = output();
  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  type = computed(() => typeOf(this.doc().name));
  rows = computed(() => {
    const d = this.doc();
    return [
      ['Extensión', this.type().ext ? '.' + this.type().ext.toLowerCase() : '—'],
      ['Tipo MIME', d.contentType],
      ['Tamaño', fmtSize(d.size)],
      ['Estado', STATUS[d.status]],
      ['Subido', fmtDate(d.uploadedAt, true)],
      ['ID', d.id],
    ];
  });

  constructor() {
    afterNextRender(() => this.dlg().nativeElement.showModal());
  }
}
