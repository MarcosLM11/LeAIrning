import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DocumentCard } from './document-card';
import { DocumentModal } from './document-modal';
import { Doc, DocumentsService } from './documents.service';
import { TYPE_KEYS, typeOf } from './file-type';

const DAY = 86_400_000;

const RANGES = [
  { label: 'Fecha: Cualquiera', days: 0 },
  { label: 'Hoy', days: 1 },
  { label: 'Últimos 7 días', days: 7 },
  { label: 'Último mes', days: 30 },
  { label: 'Último año', days: 365 },
];

@Component({
  selector: 'app-documents',
  host: { class: 'mx-auto block max-w-[1040px]' },
  imports: [FormsModule, DocumentCard, DocumentModal],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Documentos</h1>
      <p class="mt-1 text-sm text-mute">Gestiona el material con el que estudias</p>
    </header>

    <div class="flex h-24 items-center justify-between gap-4 rounded-2xl border border-dashed px-6"
         [class]="dragging() ? 'border-brand bg-app' : 'border-line bg-card'"
         (dragover)="$event.preventDefault(); dragging.set(true)" (dragleave)="dragging.set(false)" (drop)="drop($event)">
      <div class="flex items-center gap-4">
        <span class="grid size-11 place-items-center rounded-[10px] bg-app text-mute">
          <svg class="size-[22px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 14.9A7 7 0 1 1 15.7 8h1.8a4.5 4.5 0 0 1 2.5 8.2" /><path d="M12 12v9" /><path d="m16 16-4-4-4 4" />
          </svg>
        </span>
        <div>
          <p class="text-sm font-medium">Arrastra un archivo aquí</p>
          <p class="text-[13px] text-mute">{{ error() || 'PDF, MD' }}</p>
        </div>
      </div>
      <input #input type="file" hidden accept=".pdf,.md" (change)="upload(input.files?.[0])" />
      <button class="btn !w-auto px-4" (click)="input.click()">Subir archivo</button>
    </div>

    <div class="mb-5 mt-8 flex flex-wrap items-center gap-3">
      <input class="input max-w-[360px] flex-1" [(ngModel)]="search" placeholder="Buscar por nombre" />
      <select class="input select !w-auto" [(ngModel)]="type">
        <option value="">Tipo: Todos</option>
        @for (t of types; track t) { <option [value]="t">{{ t }}</option> }
      </select>
      <select class="input select !w-auto" [(ngModel)]="days">
        @for (r of ranges; track r.days) { <option [ngValue]="r.days">{{ r.label }}</option> }
      </select>
      <span class="ml-auto text-[13px] text-mute">{{ shown().length }} documentos</span>
    </div>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      @for (d of shown(); track d.id) { <app-document-card [doc]="d" (open)="selected.set(d)" (remove)="remove(d)" /> }
    </div>

    @if (selected(); as d) {
      <app-document-modal [doc]="d" (closed)="selected.set(null)" (remove)="remove(d)" />
    }
  `,
})

export class Documents {
  private service = inject(DocumentsService);
  docs = signal<Doc[]>([]);
  error = signal('');
  dragging = signal(false);
  selected = signal<Doc | null>(null);
  types = TYPE_KEYS;
  ranges = RANGES;
  search = signal('');
  type = signal('');
  days = signal(0);

  shown = computed(() => {
    const q = this.search().toLowerCase(), t = this.type(), since = Date.now() - this.days() * DAY;
    return this.docs().filter(
      (d) => d.name.toLowerCase().includes(q) && (!t || typeOf(d.name).ext === t) && (!this.days() || Date.parse(d.uploadedAt) >= since),
    );
  });

  constructor() {
    this.load();
  }

  load() {
    this.service.list().subscribe((docs) => this.docs.set([...docs].sort((a, b) => b.uploadedAt.localeCompare(a.uploadedAt))));
  }

  remove(d: Doc) {
    if (!confirm(`¿Eliminar "${d.name}"? No se puede deshacer.`)) return;
    this.service.remove(d.id).subscribe({
      next: () => { this.selected.set(null); this.load(); },
      error: () => this.error.set('No se pudo eliminar el archivo'),
    });
  }

  drop(e: DragEvent) {
    e.preventDefault();
    this.dragging.set(false);
    this.upload(e.dataTransfer?.files[0]);
  }

  upload(file?: File) {
    if (!file) return;
    this.error.set('');
    this.service.upload(file).subscribe({ next: () => this.load(), error: () => this.error.set('No se pudo subir el archivo') });
  }
}
