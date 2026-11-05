import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fmtDate, fmtSize, isReady, STATUS } from './doc-format';
import { Doc } from './documents.service';
import { FileIcon } from './file-icon';
import { typeOf } from './file-type';

@Component({
  selector: 'app-document-card',
  imports: [RouterLink, FileIcon],
  template: `
    <article role="button" tabindex="0" (click)="open.emit()" (keydown.enter)="open.emit()"
             class="flex h-full cursor-pointer flex-col gap-4 rounded-2xl border border-line bg-card p-5 hover:border-mute/40">
      <div class="flex items-start justify-between">
        <app-file-icon [name]="doc().name" />
        <button (click)="$event.stopPropagation(); remove.emit()" aria-label="Eliminar documento"
                class="-mr-1 -mt-1 grid size-8 place-items-center rounded-lg text-mute/70 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950">
          <svg class="size-[17px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" /><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /><path d="M10 11v6" /><path d="M14 11v6" />
          </svg>
        </button>
      </div>
      <div class="min-w-0">
        <p class="line-clamp-2 break-words text-sm font-medium leading-snug">{{ doc().name }}</p>
        <p class="mt-1 text-[13px] text-mute">{{ type().label }}</p>
      </div>
      <p class="flex items-center gap-2 text-xs text-mute">
        {{ size() }}<span class="size-[3px] rounded-full bg-mute/40"></span>Subido {{ date() }}
      </p>
      <div class="mt-auto flex items-center justify-between gap-3 border-t border-line pt-4">
        <span class="flex items-center gap-1.5 rounded-md bg-app px-2 py-1 text-xs font-medium">
          <span class="size-1.5 rounded-full" [class]="ready() ? 'bg-green-500' : 'bg-mute/50'"></span>{{ status() }}
        </span>
        <div class="flex gap-4 text-[13px] font-medium" [class]="ready() ? 'text-brand' : 'pointer-events-none text-mute/40'" (click)="$event.stopPropagation()">
          <a routerLink="/chat">Chat</a>
          <a routerLink="/quizzes">Cuestionario</a>
        </div>
      </div>
    </article>
  `,
})
export class DocumentCard {
  doc = input.required<Doc>();
  open = output();
  remove = output();
  type = computed(() => typeOf(this.doc().name));
  status = computed(() => STATUS[this.doc().status]);
  ready = computed(() => isReady(this.doc()));
  size = computed(() => fmtSize(this.doc().size));
  date = computed(() => fmtDate(this.doc().uploadedAt));
}
