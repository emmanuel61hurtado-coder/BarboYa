export type Block =
  | { kind: 'title' | 'heading' | 'paragraph'; text: string }
  | { kind: 'list'; items: string[] }

/** Lee solo lo que usan los documentos legales: títulos, listas y párrafos. */
export function parseDocument(source: string): Block[] {
  const blocks: Block[] = []
  for (const line of source.split('\n').map((l) => l.trim())) {
    if (!line) continue
    if (line.startsWith('## ')) blocks.push({ kind: 'heading', text: line.slice(3) })
    else if (line.startsWith('# ')) blocks.push({ kind: 'title', text: line.slice(2) })
    else if (line.startsWith('- ')) {
      const last = blocks[blocks.length - 1]
      if (last?.kind === 'list') last.items.push(line.slice(2))
      else blocks.push({ kind: 'list', items: [line.slice(2)] })
    } else blocks.push({ kind: 'paragraph', text: line })
  }
  return blocks
}
