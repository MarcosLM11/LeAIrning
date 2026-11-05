import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Documents } from './documents';
import { DocumentsService } from './documents.service';

const doc = (name: string, ago: number) => ({
  id: name, name, size: 1, status: 'READY' as const, uploadedAt: new Date(Date.now() - ago * 86_400_000).toISOString(),
});

describe('Documents filters', () => {
  it('filtra por nombre, tipo y fecha', () => {
    const docs = [doc('Biologia.pdf', 1), doc('notas.md', 1), doc('Antiguo.pdf', 100)];
    TestBed.configureTestingModule({ providers: [{ provide: DocumentsService, useValue: { list: () => of(docs) } }] });
    const page = TestBed.runInInjectionContext(() => new Documents());

    expect(page.shown().length).toBe(3);
    page.search.set('bio');
    expect(page.shown().map((d) => d.name)).toEqual(['Biologia.pdf']);
    page.search.set('');
    page.type.set('PDF');
    expect(page.shown().length).toBe(2);
    page.days.set(7);
    expect(page.shown().map((d) => d.name)).toEqual(['Biologia.pdf']);
  });
});
