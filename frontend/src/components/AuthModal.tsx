import React from 'react'
import { X } from 'lucide-react'
import { UserRole, UserSession } from '../types'
import { AuthDashboard } from './AuthDashboard'
import { sound } from '../utils/sound'

interface AuthModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (session: UserSession) => void
  initialRole?: UserRole
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  initialRole = 'customer',
}) => {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-xl animate-fade-in overflow-y-auto">
      <div className="relative w-full max-w-4xl max-h-[94vh] overflow-y-auto my-auto rounded-3xl">
        {/* Floating Close Button */}
        <button
          onClick={() => {
            sound.tick()
            onClose()
          }}
          className="absolute top-4 right-4 z-50 p-2.5 text-slate-400 hover:text-white rounded-2xl bg-slate-900/90 border border-slate-700/80 shadow-lg hover:bg-slate-800 transition active:scale-95 cursor-pointer"
          aria-label="Cerrar ventana de autenticación"
        >
          <X className="w-5 h-5" />
        </button>

        <AuthDashboard
          initialMode="login"
          initialRole={initialRole}
          onSuccess={(sess) => {
            onSuccess(sess)
            onClose()
          }}
          onCancel={onClose}
          isModal={true}
        />
      </div>
    </div>
  )
}
