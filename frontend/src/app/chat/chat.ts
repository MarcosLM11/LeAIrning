import { Component, signal } from '@angular/core';

// Boceto: datos mock, sin backend todavía.
const MESSAGES = [
  { role: 'user', text: '¿Cuáles son las diferencias principales entre Kafka y RabbitMQ?' },
  {
    role: 'assistant',
    text: 'Kafka es un log distribuido pensado para alto throughput y reprocesar eventos; RabbitMQ es un broker de colas con enrutado flexible, ideal para tareas y mensajería clásica.',
    list: [
      'Kafka conserva los mensajes en disco y los consumidores leen por offset.',
      'RabbitMQ elimina el mensaje cuando se confirma (ack).',
      'Kafka escala por particiones; RabbitMQ por colas y exchanges.',
    ],
    sources: ['arquitectura-mensajeria.pdf · p. 12', 'apuntes-kafka.docx · p. 3'],
  },
  { role: 'user', text: 'Resúmelo en una frase para una tarjeta de estudio.' },
  {
    role: 'assistant',
    text: 'Kafka = log persistente y reproducible a gran escala; RabbitMQ = cola con enrutado flexible que borra al confirmar.',
    sources: ['arquitectura-mensajeria.pdf · p. 12'],
  },
];

const CHATS = [
  { group: 'Hoy', items: ['Kafka vs RabbitMQ', 'Resumen del tema 4'] },
  { group: 'Ayer', items: ['Patrones de diseño', 'Dudas de Spring Security'] },
];

const ICON = 'fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]';

@Component({
  selector: 'app-chat',
  // Compensa el padding del shell para ocupar todo el alto.
  host: { class: 'relative -mx-8 -my-10 flex h-screen flex-col overflow-hidden', '(keydown.escape)': 'open.set(false)' },
  template: `
    <header class="flex h-14 shrink-0 items-center justify-between px-4">
      <div class="flex items-center gap-1">
        <button (click)="open.set(true)" aria-label="Ver chats" class="grid size-9 place-items-center rounded-[10px] text-mute hover:bg-card hover:text-ink">
          <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="3" rx="2" /><path d="M9 3v18" /></svg>
        </button>
        <span class="px-2 text-sm font-medium">{{ current() }}</span>
        <button class="flex h-9 items-center gap-2 rounded-[10px] px-3 text-sm text-mute hover:bg-card">
          Todos los documentos
          <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="m6 9 6 6 6-6" /></svg>
        </button>
      </div>
      <button (click)="newChat()" aria-label="Nuevo chat" class="grid size-9 place-items-center rounded-[10px] border border-line hover:bg-card">
        <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
      </button>
    </header>

    <div class="flex-1 overflow-y-auto px-6">
      <div class="mx-auto flex max-w-3xl flex-col gap-8 py-6">
        @if (!messages().length) {
          <p class="pt-24 text-center text-2xl font-semibold tracking-tight">¿En qué te ayudo hoy?</p>
        }
        @for (m of messages(); track $index) {
          @if (m.role === 'user') {
            <div class="max-w-[80%] self-end rounded-3xl bg-card px-5 py-3 text-[15px] leading-relaxed ring-1 ring-line">{{ m.text }}</div>
          } @else {
            <div class="flex gap-4">
              <span class="grid size-8 shrink-0 place-items-center rounded-full bg-brand text-white">
                <svg class="size-4 ${ICON}" viewBox="0 0 24 24"><path d="m12 3 1.9 5.8L20 10l-6.1 1.2L12 17l-1.9-5.8L4 10l6.1-1.2z" /></svg>
              </span>
              <div class="min-w-0 flex-1 text-[15px] leading-relaxed">
                <p>{{ m.text }}</p>
                @if (m.list) {
                  <ul class="mt-3 list-disc space-y-1.5 pl-5 marker:text-mute">
                    @for (i of m.list; track i) { <li>{{ i }}</li> }
                  </ul>
                }
                @if (m.sources) {
                  <div class="mt-4 flex flex-wrap gap-2">
                    @for (s of m.sources; track s) {
                      <span class="inline-flex items-center gap-1.5 rounded-full border border-line bg-card px-3 py-1 text-xs text-mute">
                        <svg class="size-3 ${ICON}" viewBox="0 0 24 24"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /></svg>
                        {{ s }}
                      </span>
                    }
                  </div>
                }
                <div class="mt-3 flex gap-1 text-mute">
                  @for (a of actions; track a.label) {
                    <button [attr.aria-label]="a.label" class="grid size-8 place-items-center rounded-lg hover:bg-card hover:text-ink">
                      <svg class="size-4 ${ICON}" viewBox="0 0 24 24">@for (d of a.icon; track d) { <path [attr.d]="d" /> }</svg>
                    </button>
                  }
                </div>
              </div>
            </div>
          }
        }
      </div>
    </div>

    <div class="shrink-0 px-6 pb-6">
      <div class="mx-auto max-w-3xl rounded-3xl border border-line bg-card p-3 shadow-sm focus-within:ring-2 focus-within:ring-brand">
        <textarea rows="1" placeholder="Pregunta algo sobre tus documentos…"
                  class="max-h-40 w-full resize-none bg-transparent px-2 py-1.5 text-[15px] outline-none placeholder:text-mute"></textarea>
        <div class="mt-1 flex items-center justify-between">
          <button aria-label="Adjuntar" class="grid size-9 place-items-center rounded-full text-mute hover:bg-app hover:text-ink">
            <svg class="size-[18px] ${ICON}" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
          </button>
          <button aria-label="Enviar" class="grid size-9 place-items-center rounded-full bg-brand text-white hover:bg-brand-hover">
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
        <input class="input" placeholder="Buscar chats" />
        @for (g of chats; track g.group) {
          <p class="mt-2 px-3 text-[11px] text-mute">{{ g.group }}</p>
          @for (c of g.items; track c) {
            <button (click)="select(c)" [class]="c === current() ? 'bg-app font-medium' : 'text-mute hover:text-ink'"
                    class="truncate rounded-[10px] px-3 py-2 text-left text-sm">{{ c }}</button>
          }
        }
      </aside>
    }
  `,
})
export class Chat {
  chats = CHATS;
  open = signal(false);
  current = signal('Kafka vs RabbitMQ');
  messages = signal(MESSAGES);
  actions = [
    { label: 'Copiar', icon: ['M8 8h12v12H8z', 'M4 16V4h12'] },
    { label: 'Regenerar', icon: ['M3 12a9 9 0 0 1 15-6.7L21 8', 'M21 3v5h-5', 'M21 12a9 9 0 0 1-15 6.7L3 16', 'M3 21v-5h5'] },
  ];

  // Mock: todos los chats muestran la misma conversación.
  select(chat: string) {
    this.current.set(chat);
    this.messages.set(MESSAGES);
    this.open.set(false);
  }

  newChat() {
    this.current.set('Nuevo chat');
    this.messages.set([]);
    this.open.set(false);
  }
}
