import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Doc, DocumentsService } from '../documents/documents.service';
import { ChatService, Conversation, Message } from './chat.service';

const DAY = 86_400_000;
const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';

@Component({
  selector: 'app-chat',
  // Compensa el padding del shell para ocupar todo el alto.
  host: { class: 'relative -mx-8 -my-10 flex h-screen flex-col overflow-hidden', '(keydown.escape)': 'open.set(false); picker.set(false)' },
  template: `
    <header class="flex h-14 shrink-0 items-center justify-between px-4">
      <div class="flex items-center gap-1">
        <button (click)="open.set(true)" aria-label="Ver chats" class="grid size-9 place-items-center rounded-[10px] text-mute hover:bg-card hover:text-ink">
          <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="3" rx="2" /><path d="M9 3v18" /></svg>
        </button>
        <span class="px-2 text-sm font-medium">{{ title() }}</span>
        <div class="relative">
          <button (click)="picker.set(!picker())" class="flex h-9 items-center gap-2 rounded-[10px] px-3 text-sm text-mute hover:bg-card">
            {{ selectedLabel() }}
            <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="m6 9 6 6 6-6" /></svg>
          </button>
          @if (picker()) {
            <div class="absolute left-0 top-10 z-10 w-72 rounded-xl border border-line bg-card p-2 shadow-sm">
              @for (d of docs(); track d.id) {
                <label class="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-app">
                  <input type="checkbox" [checked]="selected().has(d.id)" (change)="toggle(d.id)" />
                  <span class="truncate">{{ d.name }}</span>
                </label>
              } @empty {
                <p class="px-2 py-1.5 text-sm text-mute">No hay documentos listos</p>
              }
            </div>
          }
        </div>
      </div>
      <button (click)="newChat()" aria-label="Nuevo chat" class="grid size-9 place-items-center rounded-[10px] border border-line hover:bg-card">
        <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
      </button>
    </header>

    <div #scroller class="flex-1 overflow-y-auto px-6">
      <div class="mx-auto flex max-w-3xl flex-col gap-8 py-6">
        @if (!messages().length) {
          <p class="pt-24 text-center text-2xl font-semibold tracking-tight">¿En qué te ayudo hoy?</p>
        }
        @for (m of messages(); track $index) {
          @if (m.role === 'user') {
            <div class="max-w-[80%] self-end whitespace-pre-wrap rounded-3xl bg-card px-5 py-3 text-[15px] leading-relaxed ring-1 ring-line">{{ m.text }}</div>
          } @else {
            <div class="flex gap-4">
              <span class="grid size-8 shrink-0 place-items-center rounded-full bg-brand text-white">
                <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="m12 3 1.9 5.8L20 10l-6.1 1.2L12 17l-1.9-5.8L4 10l6.1-1.2z" /></svg>
              </span>
              <div class="min-w-0 flex-1 text-[15px] leading-relaxed">
                <p class="whitespace-pre-wrap">{{ m.text || '…' }}</p>
                @if (m.sources?.length) {
                  <div class="mt-4 flex flex-wrap gap-2">
                    @for (s of m.sources; track $index) {
                      <span class="inline-flex items-center gap-1.5 rounded-full border border-line bg-card px-3 py-1 text-xs text-mute">
                        <svg class="size-3 ${ICON}" viewBox="0 0 24 24"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /></svg>
                        {{ s.filename }}@if (s.page !== null) { · p. {{ s.page }} }
                      </span>
                    }
                  </div>
                }
                <div class="mt-3 flex gap-1 text-mute">
                  <button (click)="copy(m.text)" aria-label="Copiar" class="grid size-8 place-items-center rounded-lg hover:bg-card hover:text-ink">
                    <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M8 8h12v12H8z" /><path d="M4 16V4h12" /></svg>
                  </button>
                </div>
              </div>
            </div>
          }
        }
        @if (error()) { <p class="text-sm text-red-600">{{ error() }}</p> }
      </div>
    </div>

    <div class="shrink-0 px-6 pb-6">
      <div class="mx-auto max-w-3xl rounded-3xl border border-line bg-card p-3 shadow-sm focus-within:ring-2 focus-within:ring-brand">
        <textarea rows="1" placeholder="Pregunta algo sobre tus documentos…" [value]="draft()" (input)="draft.set($any($event.target).value)"
                  (keydown.enter)="$event.preventDefault(); send()"
                  class="max-h-40 w-full resize-none bg-transparent px-2 py-1.5 text-[15px] outline-none placeholder:text-mute"></textarea>
        <div class="mt-1 flex items-center justify-end">
          <button (click)="send()" [disabled]="sending() || !draft().trim()" aria-label="Enviar"
                  class="grid size-9 place-items-center rounded-full bg-brand text-white hover:bg-brand-hover disabled:opacity-50">
            <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M12 19V5M5 12l7-7 7 7" /></svg>
          </button>
        </div>
      </div>
      <p class="mt-2 text-center text-xs text-mute">Las respuestas pueden contener errores. Verifica con tus fuentes.</p>
    </div>

    @if (open()) {
      <div (click)="open.set(false)" class="absolute inset-0 z-10 bg-black/30"></div>
      <aside class="absolute inset-y-0 left-0 z-20 flex w-72 flex-col gap-2 border-r border-line bg-card p-3">
        <div class="flex items-center justify-between">
          <button (click)="newChat()" class="flex h-9 items-center gap-2 rounded-[10px] border border-line px-3 text-sm font-medium hover:bg-app">
            <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
            Nuevo chat
          </button>
          <button (click)="open.set(false)" aria-label="Cerrar" class="grid size-9 place-items-center rounded-[10px] text-mute hover:bg-app hover:text-ink">
            <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
          </button>
        </div>
        <input class="input" placeholder="Buscar chats" [value]="search()" (input)="search.set($any($event.target).value)" />
        <div class="min-h-0 flex-1 overflow-y-auto">
          @for (g of groups(); track g.group) {
            <p class="mt-2 px-3 text-[11px] text-mute">{{ g.group }}</p>
            @for (c of g.items; track c.id) {
              <div [class]="c.id === current() ? 'bg-app font-medium' : 'text-mute hover:text-ink'" class="group flex items-center rounded-[10px]">
                <button (click)="select(c.id)" class="min-w-0 flex-1 truncate px-3 py-2 text-left text-sm">{{ c.title }}</button>
                <button (click)="remove(c)" aria-label="Eliminar chat" class="mr-1 hidden size-7 place-items-center rounded-lg text-mute hover:text-red-600 group-hover:grid">
                  <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
                </button>
              </div>
            }
          }
        </div>
      </aside>
    }
  `,
})
export class Chat {
  private service = inject(ChatService);
  private scroller = viewChild.required<ElementRef<HTMLElement>>('scroller');

  open = signal(false);
  picker = signal(false);
  search = signal('');
  draft = signal('');
  error = signal('');
  sending = signal(false);
  conversations = signal<Conversation[]>([]);
  messages = signal<Message[]>([]);
  // A chat exists on the server after its first message; until then its id is just a fresh UUID.
  current = signal<string>(crypto.randomUUID());
  docs = signal<Doc[]>([]);
  selected = signal(new Set<string>());

  title = computed(() => this.conversations().find((c) => c.id === this.current())?.title ?? 'Nuevo chat');
  selectedLabel = computed(() => {
    const ids = [...this.selected()];
    if (!ids.length) return 'Todos los documentos';
    return ids.length === 1 ? (this.docs().find((d) => d.id === ids[0])?.name ?? '1 documento') : `${ids.length} documentos`;
  });
  groups = computed(() => {
    const q = this.search().toLowerCase();
    const today = new Date().setHours(0, 0, 0, 0);
    const out = new Map<string, Conversation[]>();
    for (const c of this.conversations()) {
      if (!c.title.toLowerCase().includes(q)) continue;
      const t = Date.parse(c.createdAt);
      const group = t >= today ? 'Hoy' : t >= today - DAY ? 'Ayer' : 'Anteriores';
      out.set(group, [...(out.get(group) ?? []), c]);
    }
    return [...out].map(([group, items]) => ({ group, items }));
  });

  constructor() {
    // The "Chat" link of a document opens the chat limited to that document.
    const doc = inject(ActivatedRoute).snapshot.queryParamMap.get('doc');
    if (doc) this.selected.set(new Set([doc]));
    inject(DocumentsService).list().subscribe((l) => this.docs.set(l.filter((d) => d.status === 'READY')));
    this.loadChats();
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scroller().nativeElement.scrollTo({ top: this.scroller().nativeElement.scrollHeight }));
    });
  }

  loadChats() {
    this.service.conversations().subscribe((l) => this.conversations.set(l));
  }

  private load(id: string) {
    this.service.history(id).subscribe((m) => this.messages.set(m));
  }

  toggle(id: string) {
    this.selected.update((s) => {
      const c = new Set(s);
      c.delete(id) || c.add(id);
      return c;
    });
  }

  select(id: string) {
    this.current.set(id);
    this.error.set('');
    this.load(id);
    this.open.set(false);
  }

  newChat() {
    this.current.set(crypto.randomUUID());
    this.messages.set([]);
    this.error.set('');
    this.open.set(false);
  }

  remove(c: Conversation) {
    if (!confirm(`¿Eliminar "${c.title}"? No se puede deshacer.`)) return;
    this.service.remove(c.id).subscribe(() => {
      this.loadChats();
      if (c.id === this.current()) this.newChat();
    });
  }

  copy(text: string) {
    navigator.clipboard.writeText(text);
  }

  send() {
    const text = this.draft().trim();
    if (!text || this.sending()) return;
    this.draft.set('');
    this.error.set('');
    this.sending.set(true);
    this.messages.update((m) => [...m, { role: 'user', text }, { role: 'assistant', text: '' }]);
    this.service.stream(text, this.current(), [...this.selected()]).subscribe({
      next: (chunk) => this.messages.update((m) => m.map((x, i) => (i === m.length - 1 ? { ...x, text: x.text + chunk } : x))),
      error: () => {
        this.error.set('No se pudo obtener la respuesta');
        this.messages.update((m) => (m.at(-1)?.text ? m : m.slice(0, -1)));
        this.sending.set(false);
      },
      complete: () => {
        this.sending.set(false);
        this.loadChats();
        // The stored answer carries its sources; the streamed text has none.
        this.load(this.current());
      },
    });
  }
}
