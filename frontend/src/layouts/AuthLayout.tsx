import { Outlet } from 'react-router-dom'
import { LegalFooter } from '../modules/legal/LegalFooter'

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col items-center justify-center px-4 py-10">
      <p className="font-display font-black text-3xl tracking-wider text-white mb-6">
        BARBO<span className="text-sky-400">YA</span>
      </p>
      <main className="w-full max-w-md">
        <Outlet />
      </main>
      <LegalFooter />
    </div>
  )
}
