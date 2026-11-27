import { Link } from 'react-router-dom'

/** Documentos, autoridad del consumidor (enlace exigido por la ley) y emergencias. */
export function LegalFooter() {
  return (
    <footer className="flex flex-wrap justify-center gap-x-5 gap-y-2 px-4 py-6 text-xs text-slate-500">
      <Link to="/terminos" className="hover:text-white">
        Términos
      </Link>
      <Link to="/privacidad" className="hover:text-white">
        Política de datos
      </Link>
      <a href="https://www.sic.gov.co" target="_blank" rel="noreferrer" className="hover:text-white">
        Superintendencia de Industria y Comercio
      </a>
      <a href="tel:123" className="hover:text-rose-300">
        ¿Emergencia? Llama al 123
      </a>
    </footer>
  )
}
