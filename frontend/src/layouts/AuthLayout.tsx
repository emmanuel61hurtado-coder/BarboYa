import { Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col items-center justify-center px-4 py-10">
      <p className="font-display font-black text-3xl tracking-wider text-white mb-6">
        BARBO<span className="text-sky-400">YA</span>
      </p>
      <main className="w-full max-w-md">
        <Outlet />
      </main>
      <a href="tel:123" className="mt-8 text-xs text-slate-500 hover:text-rose-300">
        ¿Emergencia? Llama al 123
      </a>
    </div>
  )
}
