import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { DocumentSlug, legalApi } from '../../services/api/legal'
import { Alert, ErrorAlert } from '../../components/ui/surface'
import { parseDocument } from './markdown'
import { LegalFooter } from './LegalFooter'

export function LegalPage({ slug }: { slug: DocumentSlug }) {
  const document = useQuery({ queryKey: ['legal', slug], queryFn: () => legalApi.document(slug) })

  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100">
      <main className="mx-auto max-w-2xl space-y-4 px-4 py-8">
        <Link to="/" className="text-sm text-sky-400 hover:underline">
          Volver a BarboYa
        </Link>
        <ErrorAlert error={document.error} />
        {document.data && !document.data.reviewed && (
          <Alert variant="warning">Borrador pendiente de revisión legal. No uses datos reales todavía.</Alert>
        )}
        {document.data &&
          parseDocument(document.data.body).map((block, index) => {
            if (block.kind === 'title') {
              return (
                <h1 key={index} className="text-2xl font-display font-black">
                  {block.text}
                </h1>
              )
            }
            if (block.kind === 'heading') {
              return (
                <h2 key={index} className="pt-3 text-lg font-bold">
                  {block.text}
                </h2>
              )
            }
            if (block.kind === 'list') {
              return (
                <ul key={index} className="list-disc space-y-1 pl-5 text-sm text-slate-300">
                  {block.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              )
            }
            return (
              <p key={index} className="break-words text-sm text-slate-300">
                {block.text}
              </p>
            )
          })}
      </main>
      <LegalFooter />
    </div>
  )
}
