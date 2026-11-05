import { Doc } from './documents.service';

export const STATUS = { READY: 'Listo', UPLOADED: 'Procesando', UPLOADING: 'Procesando', FAILED: 'Error' };

export const fmtSize = (b: number) => (b < 1048576 ? `${Math.round(b / 1024)} KB` : `${(b / 1048576).toFixed(1)} MB`);

export const fmtDate = (iso: string, withTime = false) =>
  new Date(iso).toLocaleString('es', withTime ? { dateStyle: 'medium', timeStyle: 'short' } : { day: 'numeric', month: 'short', year: 'numeric' });

export const isReady = (d: Doc) => d.status === 'READY';
