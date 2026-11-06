import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Documents } from './documents';
import { DocumentsService } from './documents.service';

const doc = (name: string) => ({ id: name, name, contentType: 'x', size: 1, status: 'READY' as const, uploadedAt: new Date().toISOString() });
const page = (content: ReturnType<typeof doc>[], number = 0, totalPages = 1) =>
  of({ content, page: { size: 12, number, totalElements: content.length, totalPages } });

describe('Documents paging', () => {
  it('pide la página con los filtros y vuelve atrás si se vacía la última', () => {
    const pageFn = vi.fn().mockReturnValueOnce(page([doc('a.pdf')], 1, 2)).mockReturnValueOnce(page([])).mockReturnValue(page([doc('a.pdf')], 0, 1));
    TestBed.configureTestingModule({ providers: [{ provide: DocumentsService, useValue: { page: pageFn } }] });
    const c = TestBed.runInInjectionContext(() => new Documents());

    c.pageIndex.set(1);
    c.search.set(' bio ');
    c.type.set('PDF');
    c.load();
    expect(pageFn.mock.calls[0][0]).toMatchObject({ page: 1, size: 12, name: 'bio', type: 'PDF' });
    expect(c.docs().length).toBe(1);

    c.load(); // page 1 now empty -> steps back to page 0
    expect(c.pageIndex()).toBe(0);
    expect(c.totalPages()).toBe(1);
  });
});
