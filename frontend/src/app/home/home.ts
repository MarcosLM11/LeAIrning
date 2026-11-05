import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  host: { class: 'mx-auto block max-w-[960px]' },
  imports: [RouterLink],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Bienvenido</h1>
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
  `,
})
export class Home {
  actions = [
    { path: '/chat', title: 'Chatear con tus documentos', text: 'Haz preguntas y obtén respuestas al instante' },
    { path: '/quizzes', title: 'Generar un cuestionario', text: 'Crea preguntas a partir de tu material' },
  ];
}
