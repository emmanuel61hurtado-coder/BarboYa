import { describe, expect, it } from 'vitest'
import { parseDocument } from './markdown'

describe('documentos legales', () => {
  it('separa títulos, listas y párrafos sin perder texto', () => {
    expect(parseDocument('# Uno\r\n\r\nTexto <b>literal</b>\n## Dos\n- a\n- b\n\nFin')).toEqual([
      { kind: 'title', text: 'Uno' },
      { kind: 'paragraph', text: 'Texto <b>literal</b>' },
      { kind: 'heading', text: 'Dos' },
      { kind: 'list', items: ['a', 'b'] },
      { kind: 'paragraph', text: 'Fin' },
    ])
  })
})
