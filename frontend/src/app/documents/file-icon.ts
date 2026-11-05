import { Component, computed, input } from '@angular/core';
import { typeOf } from './file-type';

@Component({
  selector: 'app-file-icon',
  template: `
    <div class="relative grid size-11 shrink-0 place-items-center rounded-[10px]" [class]="type().color">
      <svg class="size-[22px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /><path d="M16 13H8" /><path d="M16 17H8" />
      </svg>
      @if (type().ext) {
        <span class="absolute -bottom-1.5 -right-2 rounded-[5px] border border-card px-1.5 py-0.5 text-[10px] font-semibold tracking-wide" [class]="type().color">{{ type().ext }}</span>
      }
    </div>
  `,
})
export class FileIcon {
  name = input.required<string>();
  type = computed(() => typeOf(this.name()));
}
