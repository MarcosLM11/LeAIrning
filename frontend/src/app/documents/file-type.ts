// Colores y etiqueta por extensión. Los textos de las clases son literales para que Tailwind los detecte.
const TYPES: Record<string, { label: string; color: string }> = {
  PDF: { label: 'Documento PDF', color: 'bg-red-50 text-red-600 dark:bg-red-950 dark:text-red-400' },
  DOCX: { label: 'Documento de Word', color: 'bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-400' },
  TXT: { label: 'Texto plano', color: 'bg-neutral-100 text-neutral-500 dark:bg-neutral-800 dark:text-neutral-400' },
  CSV: { label: 'Datos CSV', color: 'bg-green-50 text-green-600 dark:bg-green-950 dark:text-green-400' },
  MD: { label: 'Markdown', color: 'bg-violet-50 text-violet-600 dark:bg-violet-950 dark:text-violet-400' },
};

export const TYPE_KEYS = Object.keys(TYPES);

export function typeOf(name: string) {
  const ext = name.includes('.') ? name.split('.').pop()!.toUpperCase() : '';
  return { ext, ...(TYPES[ext] ?? { label: 'Documento', color: 'bg-app text-mute' }) };
}
