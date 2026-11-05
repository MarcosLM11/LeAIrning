import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Doc } from './documents.service';
import { typeOf } from './file-type';

const STATUS = { UPLOADED: 'Listo', UPLOADING: 'Procesando', FAILED: 'Error' };

@Component({
  selector: 'app-document-card',
  imports: [RouterLink],
  template: `
    <article class="flex flex-col gap-4 rounded-2xl border border-line bg-card p-5 hover:border-mute/40">
      <div class="relative grid size-11 place-items-center rounded-[10px]" [class]="type().color">
        <svg class="size-[22px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /><path d="M16 13H8" /><path d="M16 17H8" />
        </svg>
        @if (type().ext) {
          <span class="absolute -bottom-1.5 -right-2 rounded-[5px] border border-card px-1.5 py-0.5 text-[10px] font-semibold tracking-wide" [class]="type().color">{{ type().ext }}</span>
        }
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
        <div class="flex gap-4 text-[13px] font-medium" [class]="ready() ? 'text-brand' : 'pointer-events-none text-mute/40'">
          <a routerLink="/chat">Chat</a>
          <a routerLink="/quizzes">Cuestionario</a>
        </div>
      </div>
    </article>
  `,
})

export class DocumentCard {
  doc = input.required<Doc>();
  type = computed(() => typeOf(this.doc().name));
  status = computed(() => STATUS[this.doc().status]);
  ready = computed(() => this.doc().status === 'UPLOADED');
  size = computed(() => {
    const b = this.doc().size;
    return b < 1048576 ? `${Math.round(b / 1024)} KB` : `${(b / 1048576).toFixed(1)} MB`;
  });
  date = computed(() => new Date(this.doc().uploadedAt).toLocaleDateString('es', { day: 'numeric', month: 'short', year: 'numeric' }));
}
