import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Datos de ejemplo hasta conectar con el backend.
const DOCS = [
  { name: 'Apuntes_Biologia_Celular.pdf', date: 'hace 2 horas', ready: true },
  { name: 'Resumen_Historia_Universal.docx', date: 'hace 1 día', ready: true },
  { name: 'Contrato_Arrendamiento.pdf', date: 'hace 3 días', ready: false },
  { name: 'Tesis_Capitulo3.pdf', date: 'hace 5 días', ready: true },
];

const QUIZZES = [
  { title: 'Biología Celular', level: 'Fácil', score: '9/10' },
  { title: 'Historia Universal', level: 'Media', score: '7/10' },
  { title: 'Tesis Capítulo 3', level: 'Difícil', score: '5/10' },
];

@Component({
  selector: 'app-home',
  host: { class: 'mx-auto block max-w-[960px]' },
  imports: [RouterLink],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Hola, Marcos</h1>
      <p class="mt-1 text-sm text-mute">Sube un documento y empieza a aprender</p>
    </header>

    <div class="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-line bg-card px-6 py-16">
      <p class="text-sm font-medium">Arrastra un PDF o documento aquí</p>
      <button class="h-10 rounded-[10px] border border-line px-4 text-sm font-medium hover:bg-app">Seleccionar archivo</button>
      <p class="text-[13px] text-mute">PDF, DOCX, TXT</p>
    </div>

    <div class="mt-8 grid gap-4 md:grid-cols-2">
      @for (a of actions; track a.path) {
        <a [routerLink]="a.path" class="rounded-2xl border border-line bg-card p-5 hover:border-mute/40">
          <p class="text-sm font-semibold">{{ a.title }}</p>
          <p class="mt-0.5 text-[13px] text-mute">{{ a.text }}</p>
        </a>
      }
    </div>

    <h2 class="mb-3 mt-10 text-[15px] font-semibold tracking-tight">Documentos recientes</h2>
    <ul class="divide-y divide-line rounded-2xl border border-line bg-card">
      @for (d of docs; track d.name) {
        <li class="flex items-center justify-between gap-4 px-5 py-4">
          <div class="min-w-0">
            <p class="truncate text-sm font-medium">{{ d.name }}</p>
            <p class="text-[13px] text-mute">{{ d.date }}</p>
          </div>
          <span class="flex shrink-0 items-center gap-1.5 rounded-md bg-app px-2 py-1 text-xs font-medium">
            <span class="size-1.5 rounded-full" [class]="d.ready ? 'bg-green-500' : 'bg-mute/50'"></span>
            {{ d.ready ? 'Listo' : 'Procesando' }}
          </span>
        </li>
      }
    </ul>

    <h2 class="mb-3 mt-10 text-[15px] font-semibold tracking-tight">Cuestionarios recientes</h2>
    <ul class="divide-y divide-line rounded-2xl border border-line bg-card">
      @for (q of quizzes; track q.title) {
        <li class="flex items-center justify-between px-5 py-3.5 text-sm">
          <span class="font-medium">{{ q.title }} <span class="ml-2 rounded-md bg-app px-2 py-0.5 text-xs text-mute">{{ q.level }}</span></span>
          <span class="font-semibold">{{ q.score }}</span>
        </li>
      }
    </ul>
  `,
})

export class Home {
  docs = DOCS;
  quizzes = QUIZZES;
  actions = [
    { path: '/chat', title: 'Chatear con tus documentos', text: 'Haz preguntas y obtén respuestas al instante' },
    { path: '/quizzes', title: 'Generar un cuestionario', text: 'Crea preguntas a partir de tu material' },
  ];
}
