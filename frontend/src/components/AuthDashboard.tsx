import React, { useState, useEffect } from 'react'
import {
  User, Bike, Store, ShieldCheck, Mail, Phone, Lock, Eye, EyeOff,
  ArrowRight, CheckCircle2, AlertCircle, KeyRound, Sparkles, RefreshCw,
  Shield, Car, Check, ChevronRight, HelpCircle, Smartphone, AtSign
} from 'lucide-react'
import { api } from '../utils/api'
import { sound } from '../utils/sound'
import { UserRole, UserSession } from '../types'

interface AuthDashboardProps {
  initialMode?: 'login' | 'register' | 'recover'
  initialRole?: UserRole
  onSuccess: (session: UserSession) => void
  onCancel?: () => void
  isModal?: boolean
}

type SelectableRole = 'customer' | 'driver' | 'merchant_owner' | 'admin'
type RecoveryMethod = 'phone' | 'email'

const ROLE_DATA: Record<SelectableRole, {
  label: string
  title: string
  subtitle: string
  icon: any
  accentColor: string
  borderClass: string
  bgClass: string
  badgeClass: string
  benefits: string[]
}> = {
  customer: {
    label: 'Pasajero',
    title: 'Cliente / Pasajero',
    subtitle: 'Viajes seguros, comida local y envíos express en Barbosa',
    icon: User,
    accentColor: '#0ea5e9',
    borderClass: 'border-sky-500/40 hover:border-sky-500',
    bgClass: 'bg-sky-500/10',
    badgeClass: 'bg-sky-500/15 text-sky-400 border-sky-500/30',
    benefits: [
      'Negocia tarifas justas sin cobros ocultos ni dinámicas abusivas',
      'Pide en tus restaurantes favoritos de Barbosa con tracking real',
      'Botón táctico de SOS conectado a la red local 24/7'
    ]
  },
  driver: {
    label: 'Conductor',
    title: 'Conductor / Repartidor',
    subtitle: 'Gana el 100% de tu tarifa pactada sin comisiones abusivas',
    icon: Bike,
    accentColor: '#10b981',
    borderClass: 'border-emerald-500/40 hover:border-emerald-500',
    bgClass: 'bg-emerald-500/10',
    badgeClass: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
    benefits: [
      'Conserva la totalidad de lo que cobras al pasajero',
      'Sistema de bolsa prepago transparente de bajo costo',
      'Filtro exclusivo para conductoras mujeres y verificación de identidad'
    ]
  },
  merchant_owner: {
    label: 'Comercio',
    title: 'Comercio / Restaurante',
    subtitle: 'Gestiona tu menú, recibe pedidos y despacha sin intermediarios caros',
    icon: Store,
    accentColor: '#f97316',
    borderClass: 'border-orange-500/40 hover:border-orange-500',
    bgClass: 'bg-orange-500/10',
    badgeClass: 'bg-orange-500/15 text-orange-400 border-orange-500/30',
    benefits: [
      'Digitaliza tu negocio en Barbosa en menos de 5 minutos',
      'Asignación directa con repartidores locales disponibles',
      'Estadísticas de ventas diarias y control total de inventario'
    ]
  },
  admin: {
    label: 'Admin',
    title: 'Central / Administrador',
    subtitle: 'Auditoría, telemetría y verificación de conductores de Barbosa',
    icon: ShieldCheck,
    accentColor: '#8b5cf6',
    borderClass: 'border-purple-500/40 hover:border-purple-500',
    bgClass: 'bg-purple-500/10',
    badgeClass: 'bg-purple-500/15 text-purple-400 border-purple-500/30',
    benefits: [
      'Verificación documental estricta de licencias y SOAT',
      'Supervisión de geocerca en tiempo real de Santander',
      'Acceso exclusivo con auditoría de seguridad cifrada'
    ]
  }
}

export const AuthDashboard: React.FC<AuthDashboardProps> = ({
  initialMode = 'login',
  initialRole = 'customer',
  onSuccess,
  onCancel,
  isModal = false,
}) => {
  const [mode, setMode] = useState<'login' | 'register' | 'recover'>(initialMode)
  const [selectedRole, setSelectedRole] = useState<SelectableRole>(
    initialRole === 'admin' || initialRole === 'superadmin'
      ? 'admin'
      : initialRole === 'driver' || initialRole === 'courier'
      ? 'driver'
      : initialRole === 'merchant_owner' || initialRole === 'merchant_employee'
      ? 'merchant_owner'
      : 'customer'
  )

  // Recovery method: phone or email
  const [recoveryMethod, setRecoveryMethod] = useState<RecoveryMethod>('email')
  const [recoveryIdentifier, setRecoveryIdentifier] = useState('')
  const [recoveryPin, setRecoveryPin] = useState('')
  const [recoveryNewPassword, setRecoveryNewPassword] = useState('')
  const [recoveryConfirmPassword, setRecoveryConfirmPassword] = useState('')
  const [codeRequested, setCodeRequested] = useState(false)
  const [simulatedCode, setSimulatedCode] = useState<string | null>(null)

  // Common inputs
  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(true)

  // Register inputs
  const [fullName, setFullName] = useState('')
  const [registerPhone, setRegisterPhone] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [acceptTerms, setAcceptTerms] = useState(true)

  // Driver fields
  const [vehicleType, setVehicleType] = useState<'moto' | 'car'>('moto')
  const [vehiclePlate, setVehiclePlate] = useState('')
  const [documentId, setDocumentId] = useState('')

  // Merchant fields
  const [storeName, setStoreName] = useState('')
  const [storeCategory, setStoreCategory] = useState('restaurant')
  const [storeAddress, setStoreAddress] = useState('')

  // Status feedback
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  useEffect(() => {
    if (initialRole) {
      if (initialRole === 'admin' || initialRole === 'superadmin') setSelectedRole('admin')
      else if (initialRole === 'driver' || initialRole === 'courier') setSelectedRole('driver')
      else if (initialRole === 'merchant_owner' || initialRole === 'merchant_employee') setSelectedRole('merchant_owner')
      else setSelectedRole('customer')
    }
  }, [initialRole])

  const currentRoleConfig = ROLE_DATA[selectedRole]

  // Quick 1-click demo filler
  const handleFillDemo = (role: SelectableRole) => {
    sound.tick()
    setSelectedRole(role)
    if (role === 'customer') {
      setLoginIdentifier('+573001112233')
      setPassword('Cliente123!')
    } else if (role === 'driver') {
      setLoginIdentifier('+573104445566')
      setPassword('Driver123!')
    } else if (role === 'merchant_owner') {
      setLoginIdentifier('+573207778899')
      setPassword('Comercio123!')
    } else if (role === 'admin') {
      setLoginIdentifier('+573009998877')
      setPassword('Admin123!')
    }
    setError(null)
  }

  // Request recovery code
  const handleRequestRecoveryCode = async () => {
    if (!recoveryIdentifier.trim()) {
      setError(
        recoveryMethod === 'email'
          ? 'Por favor ingresa tu correo electrónico registrado'
          : 'Por favor ingresa tu número de teléfono registrado (+57...)'
      )
      sound.alert()
      return
    }

    setLoading(true)
    setError(null)
    setSuccessMsg(null)

    try {
      const res = await api.requestRecoveryCode(recoveryIdentifier.trim(), recoveryMethod)
      sound.success()
      setCodeRequested(true)
      setSimulatedCode(res.security_pin || '8492')
      setRecoveryPin(res.security_pin || '8492')
      setSuccessMsg(`Código generado: ${res.security_pin || '8492'}. Se ha transmitido a ${res.masked_identifier}.`)
    } catch (err: any) {
      sound.alert()
      setError(err.message || 'No se pudo generar el código. Verifica el dato ingresado.')
    } finally {
      setLoading(false)
    }
  }

  // Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccessMsg(null)
    setLoading(true)

    try {
      if (mode === 'login') {
        if (!loginIdentifier.trim() || !password) {
          throw new Error('Por favor ingresa tu teléfono o correo y contraseña')
        }

        const res = await api.login(loginIdentifier.trim(), password, selectedRole)
        sound.success()
        const me = await api.getMe()
        const session: UserSession = {
          token: res.access_token,
          role: res.role as UserRole,
          userId: res.user_id,
          fullName: me.full_name,
          phone: me.phone,
          email: me.email,
          avatarUrl: me.avatar_url,
          gender: me.gender,
          driverInfo: me.driver_info,
          merchantInfo: me.merchant_info,
        }
        localStorage.setItem('barboya_user_session', JSON.stringify(session))
        onSuccess(session)
      } else if (mode === 'register') {
        if (selectedRole === 'admin') {
          throw new Error('El rol administrativo no permite auto-registro público.')
        }
        if (!fullName.trim() || !registerPhone.trim() || !password) {
          throw new Error('Por favor completa todos los campos obligatorios.')
        }
        if (!acceptTerms) {
          throw new Error('Debes aceptar los términos y condiciones del servicio.')
        }

        const res = await api.register({
          fullName: fullName.trim(),
          phone: registerPhone.trim(),
          email: registerEmail.trim() ? registerEmail.trim() : undefined,
          password,
          role: selectedRole,
          vehicleType: selectedRole === 'driver' ? vehicleType : undefined,
          vehiclePlate: selectedRole === 'driver' ? vehiclePlate.trim().toUpperCase() : undefined,
          documentId: selectedRole === 'driver' ? documentId.trim() : undefined,
        })

        if (selectedRole === 'merchant_owner') {
          try {
            await api.registerMerchant({
              name: storeName.trim() || `Comercio de ${fullName}`,
              category: storeCategory,
              addressNote: storeAddress.trim() || 'Barbosa Centro, Santander',
              location: { lat: 5.9328, lng: -73.6204 },
              phone: registerPhone.trim(),
            })
          } catch (mErr) {
            console.warn('Auto-registro de tienda omitido o pendiente:', mErr)
          }
        }

        sound.success()
        const me = await api.getMe()
        const session: UserSession = {
          token: res.access_token,
          role: res.role as UserRole,
          userId: res.user_id,
          fullName: me.full_name,
          phone: me.phone,
          email: me.email,
          avatarUrl: me.avatar_url,
          gender: me.gender,
          driverInfo: me.driver_info,
          merchantInfo: me.merchant_info,
        }
        localStorage.setItem('barboya_user_session', JSON.stringify(session))
        onSuccess(session)
      } else if (mode === 'recover') {
        if (!recoveryIdentifier.trim()) {
          throw new Error('Ingresa tu teléfono o correo registrado')
        }
        if (!recoveryNewPassword || recoveryNewPassword.length < 6) {
          throw new Error('La nueva contraseña debe tener al menos 6 caracteres')
        }
        if (recoveryNewPassword !== recoveryConfirmPassword) {
          throw new Error('Las contraseñas no coinciden')
        }

        await api.recoverPassword({
          identifier: recoveryIdentifier.trim(),
          recoveryMethod,
          newPassword: recoveryNewPassword,
          securityPin: recoveryPin || '8492',
        })

        sound.success()
        setSuccessMsg('¡Contraseña actualizada exitosamente! Ya puedes iniciar sesión con tu nueva clave.')
        setLoginIdentifier(recoveryIdentifier.trim())
        setPassword(recoveryNewPassword)
        setMode('login')
      }
    } catch (err: any) {
      sound.alert()
      setError(err.message || 'Error en la operación. Verifica los datos ingresados.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`w-full ${isModal ? 'max-w-4xl mx-auto' : 'max-w-6xl mx-auto py-6 sm:py-10'}`}>
      {/* Top Banner Navigation */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-4 sm:p-6 mb-6 shadow-xl backdrop-blur-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-500 via-indigo-600 to-emerald-500 p-0.5 shadow-lg">
              <div className="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center font-display font-black text-white text-xl">
                B
              </div>
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-xl sm:text-2xl font-display font-black text-white tracking-wide">
                  BARBO<span className="text-sky-400">YA</span>
                </h1>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-sky-500/10 text-sky-400 border border-sky-500/20">
                  PORTAL SEGURO
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Ecosistema digital de movilidad, comercio y logística · Barbosa, Santander
              </p>
            </div>
          </div>

          {/* Mode Switcher Tabs */}
          <div className="flex items-center bg-slate-950 p-1.5 rounded-2xl border border-slate-800/80 self-start sm:self-auto">
            <button
              type="button"
              onClick={() => {
                setMode('login')
                setError(null)
                setSuccessMsg(null)
                sound.tick()
              }}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                mode === 'login'
                  ? 'bg-sky-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>Iniciar Sesión</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setMode('register')
                setError(null)
                setSuccessMsg(null)
                sound.tick()
              }}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                mode === 'register'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>Crear Cuenta</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setMode('recover')
                setError(null)
                setSuccessMsg(null)
                sound.tick()
              }}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                mode === 'recover'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <KeyRound className="w-3.5 h-3.5" />
              <span>Recuperar</span>
            </button>
          </div>
        </div>
      </div>

      {/* Main Dual-Column Arena */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Brand & Security Guarantee Panel (5 Cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="pro-surface-elevated rounded-3xl p-5 sm:p-6 border border-slate-800 space-y-5">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider flex items-center space-x-1.5">
                <Shield className="w-3.5 h-3.5 text-emerald-400" />
                <span>Protocolo de Confianza</span>
              </span>
              <span className="text-[10px] font-mono bg-emerald-500/10 text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-500/20">
                Activo
              </span>
            </div>

            <div>
              <h3 className="text-lg font-bold text-white mb-1">
                {mode === 'login' && 'Bienvenido de vuelta a tu plataforma'}
                {mode === 'register' && 'Únete a la mayor red de Barbosa'}
                {mode === 'recover' && 'Centro de recuperación y seguridad'}
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                {mode === 'login' && 'Accede a tus servicios con total privacidad y validación instantánea por perfil.'}
                {mode === 'register' && 'Crea tu perfil en segundos y aprovecha las ventajas de un sistema sin comisiones abusivas.'}
                {mode === 'recover' && 'Restaura el acceso a tu cuenta fácilmente mediante tu correo o tu celular registrado.'}
              </p>
            </div>

            {/* Role Profile Info Card */}
            {mode !== 'recover' && (
              <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800/90 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className={`p-2 rounded-xl ${currentRoleConfig.bgClass}`}>
                      <currentRoleConfig.icon className="w-4 h-4 text-white" />
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-white">{currentRoleConfig.title}</h4>
                      <p className="text-[11px] text-slate-400">{currentRoleConfig.subtitle}</p>
                    </div>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/70 space-y-2">
                  {currentRoleConfig.benefits.map((b, i) => (
                    <div key={i} className="flex items-start space-x-2 text-[11px] text-slate-300">
                      <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                      <span>{b}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Quick Demo Credentials Pill (For instant tester review) */}
            {mode === 'login' && (
              <div className="p-3.5 rounded-2xl bg-indigo-950/30 border border-indigo-500/20 space-y-2">
                <div className="flex items-center justify-between text-xs text-indigo-300 font-bold">
                  <span className="flex items-center space-x-1">
                    <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                    <span>Acceso rápido de demostración:</span>
                  </span>
                  <span className="text-[10px] font-mono text-indigo-400">1-clic</span>
                </div>
                <div className="grid grid-cols-2 gap-1.5 text-[11px]">
                  <button
                    type="button"
                    onClick={() => handleFillDemo('customer')}
                    className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-sky-500/50 text-slate-300 hover:text-white transition text-left truncate"
                  >
                    🚗 Demo Pasajero
                  </button>
                  <button
                    type="button"
                    onClick={() => handleFillDemo('driver')}
                    className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-emerald-500/50 text-slate-300 hover:text-white transition text-left truncate"
                  >
                    🛵 Demo Conductor
                  </button>
                  <button
                    type="button"
                    onClick={() => handleFillDemo('merchant_owner')}
                    className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-orange-500/50 text-slate-300 hover:text-white transition text-left truncate"
                  >
                    🏪 Demo Comercio
                  </button>
                  <button
                    type="button"
                    onClick={() => handleFillDemo('admin')}
                    className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-purple-500/50 text-slate-300 hover:text-white transition text-left truncate"
                  >
                    🛡️ Demo Admin
                  </button>
                </div>
              </div>
            )}

            {/* Recovery Highlights */}
            {mode === 'recover' && (
              <div className="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 space-y-2 text-xs text-slate-400">
                <div className="flex items-center space-x-2 text-slate-200 font-semibold">
                  <HelpCircle className="w-4 h-4 text-sky-400" />
                  <span>¿Cómo funciona la recuperación?</span>
                </div>
                <p className="text-[11px] leading-relaxed">
                  Puedes elegir restablecer tu clave mediante el <strong>correo electrónico registrado</strong> o mediante tu <strong>número celular (WhatsApp/SMS)</strong>. Te proporcionamos un código de seguridad para confirmar tu titularidad.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Interactive Form (7 Cols) */}
        <div className="lg:col-span-7">
          <div className="pro-surface-elevated rounded-3xl p-5 sm:p-8 border border-slate-800 shadow-2xl relative">
            {/* Feedback Messages */}
            {error && (
              <div className="mb-5 p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-start space-x-3 text-xs text-rose-300 animate-shake">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <div className="flex-1 font-medium">{error}</div>
              </div>
            )}

            {successMsg && (
              <div className="mb-5 p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-start space-x-3 text-xs text-emerald-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <div className="flex-1 font-medium">{successMsg}</div>
              </div>
            )}

            {/* Form Tag Header */}
            <div className="mb-6">
              <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block mb-1">
                {mode === 'login' && 'ACCESO A TU CUENTA'}
                {mode === 'register' && 'REGISTRO DE USUARIO NUEVO'}
                {mode === 'recover' && 'RESTABLECIMIENTO DE CREDENCIALES'}
              </span>
              <h2 className="text-2xl font-bold text-white">
                {mode === 'login' && 'Iniciar Sesión'}
                {mode === 'register' && 'Crear Cuenta en BarboYa'}
                {mode === 'recover' && 'Recuperar Contraseña'}
              </h2>
            </div>

            {/* Role Selectors for Login & Register */}
            {mode !== 'recover' && (
              <div className="mb-6">
                <label className="block text-xs font-semibold text-slate-300 mb-2.5">
                  Selecciona el tipo de cuenta:
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {(['customer', 'driver', 'merchant_owner', 'admin'] as SelectableRole[]).map((r) => {
                    const conf = ROLE_DATA[r]
                    const IconComp = conf.icon
                    const isSelected = selectedRole === r
                    const isDisabled = mode === 'register' && r === 'admin'

                    return (
                      <button
                        key={r}
                        type="button"
                        disabled={isDisabled}
                        onClick={() => {
                          if (!isDisabled) {
                            setSelectedRole(r)
                            sound.tick()
                            setError(null)
                          }
                        }}
                        className={`p-3 rounded-2xl border text-center transition flex flex-col items-center space-y-1.5 relative ${
                          isDisabled
                            ? 'opacity-30 cursor-not-allowed bg-slate-900 border-slate-800'
                            : isSelected
                            ? `${conf.bgClass} ${conf.borderClass} text-white font-bold shadow-md`
                            : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                        }`}
                      >
                        <IconComp className="w-4 h-4" />
                        <span className="text-xs font-semibold">{conf.label}</span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )}

            {/* FORM BODY */}
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* ======================= LOGIN VIEW ======================= */}
              {mode === 'login' && (
                <>
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">
                      Teléfono o Correo Electrónico Registrado
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                        <User className="w-4 h-4" />
                      </div>
                      <input
                        type="text"
                        value={loginIdentifier}
                        onChange={(e) => setLoginIdentifier(e.target.value)}
                        placeholder="Ej: +57 310 123 4567 o usuario@correo.com"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-sans"
                        required
                      />
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1">
                      Puedes identificarte con el número celular o con tu correo registrado.
                    </p>
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-medium text-slate-300">
                        Contraseña
                      </label>
                      <button
                        type="button"
                        onClick={() => {
                          setMode('recover')
                          sound.tick()
                        }}
                        className="text-[11px] text-sky-400 hover:underline"
                      >
                        ¿Olvidaste tu contraseña?
                      </button>
                    </div>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                        <Lock className="w-4 h-4" />
                      </div>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••"
                        className="w-full pl-10 pr-10 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono"
                        required
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-white"
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs pt-1">
                    <label className="flex items-center space-x-2 text-slate-400 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={rememberMe}
                        onChange={(e) => setRememberMe(e.target.checked)}
                        className="rounded border-slate-700 text-sky-500 focus:ring-sky-500/20 bg-slate-900"
                      />
                      <span>Recordar este dispositivo</span>
                    </label>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-bold text-xs sm:text-sm tracking-wide shadow-lg shadow-sky-600/20 transition-all flex items-center justify-center space-x-2 active:scale-[0.99] disabled:opacity-50 cursor-pointer"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <span>Ingresar como {currentRoleConfig.label}</span>
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </>
              )}

              {/* ======================= REGISTER VIEW ======================= */}
              {mode === 'register' && (
                <>
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">
                      Nombre Completo *
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                        <User className="w-4 h-4" />
                      </div>
                      <input
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="Ej: Laura Gómez Pérez"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1.5">
                        Teléfono Celular *
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                          <Phone className="w-4 h-4" />
                        </div>
                        <input
                          type="tel"
                          value={registerPhone}
                          onChange={(e) => setRegisterPhone(e.target.value)}
                          placeholder="+57 310 123 4567"
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono"
                          required
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1.5">
                        Correo Electrónico (Recomendado)
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                          <Mail className="w-4 h-4" />
                        </div>
                        <input
                          type="email"
                          value={registerEmail}
                          onChange={(e) => setRegisterEmail(e.target.value)}
                          placeholder="tu@correo.com"
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm"
                        />
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">
                      Crear Contraseña *
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                        <Lock className="w-4 h-4" />
                      </div>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Mínimo 6 caracteres"
                        className="w-full pl-10 pr-10 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono"
                        required
                        minLength={6}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-white"
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  {/* Driver Specific Fields */}
                  {selectedRole === 'driver' && (
                    <div className="p-4 rounded-2xl bg-emerald-950/20 border border-emerald-500/20 space-y-3">
                      <h4 className="text-xs font-bold text-emerald-300 flex items-center space-x-1.5">
                        <Bike className="w-4 h-4" />
                        <span>Datos del Vehículo y Documentos</span>
                      </h4>

                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                        <div>
                          <label className="block text-[11px] text-slate-400 mb-1">Vehículo</label>
                          <select
                            value={vehicleType}
                            onChange={(e) => setVehicleType(e.target.value as any)}
                            className="w-full py-2 px-2.5 rounded-xl pro-input text-xs"
                          >
                            <option value="moto">Motocicleta</option>
                            <option value="car">Automóvil</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-[11px] text-slate-400 mb-1">Placa</label>
                          <input
                            type="text"
                            value={vehiclePlate}
                            onChange={(e) => setVehiclePlate(e.target.value.toUpperCase())}
                            placeholder="ABC12D"
                            className="w-full py-2 px-2.5 rounded-xl pro-input text-xs font-mono uppercase"
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-[11px] text-slate-400 mb-1">Cédula</label>
                          <input
                            type="text"
                            value={documentId}
                            onChange={(e) => setDocumentId(e.target.value)}
                            placeholder="1098..."
                            className="w-full py-2 px-2.5 rounded-xl pro-input text-xs font-mono"
                            required
                          />
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Merchant Specific Fields */}
                  {selectedRole === 'merchant_owner' && (
                    <div className="p-4 rounded-2xl bg-orange-950/20 border border-orange-500/20 space-y-3">
                      <h4 className="text-xs font-bold text-orange-300 flex items-center space-x-1.5">
                        <Store className="w-4 h-4" />
                        <span>Información de tu Establecimiento</span>
                      </h4>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                        <div>
                          <label className="block text-[11px] text-slate-400 mb-1">Nombre Comercial</label>
                          <input
                            type="text"
                            value={storeName}
                            onChange={(e) => setStoreName(e.target.value)}
                            placeholder="Ej: Arepas Donde Carlos"
                            className="w-full py-2 px-2.5 rounded-xl pro-input text-xs"
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-[11px] text-slate-400 mb-1">Categoría</label>
                          <select
                            value={storeCategory}
                            onChange={(e) => setStoreCategory(e.target.value)}
                            className="w-full py-2 px-2.5 rounded-xl pro-input text-xs"
                          >
                            <option value="restaurant">Restaurante / Comidas</option>
                            <option value="bakery">Panadería / Café</option>
                            <option value="supermarket">Tienda / Mercado</option>
                            <option value="pharmacy">Farmacia / Droguería</option>
                          </select>
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] text-slate-400 mb-1">Dirección en Barbosa</label>
                        <input
                          type="text"
                          value={storeAddress}
                          onChange={(e) => setStoreAddress(e.target.value)}
                          placeholder="Ej: Carrera 8 # 9-45 Centro"
                          className="w-full py-2 px-2.5 rounded-xl pro-input text-xs"
                        />
                      </div>
                    </div>
                  )}

                  <div className="pt-1">
                    <label className="flex items-center space-x-2 text-xs text-slate-400 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={acceptTerms}
                        onChange={(e) => setAcceptTerms(e.target.checked)}
                        className="rounded border-slate-700 text-emerald-500 focus:ring-emerald-500/20 bg-slate-900"
                      />
                      <span>Acepto los términos de servicio y política de privacidad de BarboYa</span>
                    </label>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs sm:text-sm tracking-wide shadow-lg shadow-emerald-600/20 transition-all flex items-center justify-center space-x-2 active:scale-[0.99] disabled:opacity-50 cursor-pointer"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <span>Crear Cuenta de {currentRoleConfig.label}</span>
                        <Check className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </>
              )}

              {/* ======================= RECOVER PASSWORD VIEW ======================= */}
              {mode === 'recover' && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-2">
                      Selecciona el método de recuperación:
                    </label>
                    <div className="grid grid-cols-2 gap-3 mb-4">
                      <button
                        type="button"
                        onClick={() => {
                          setRecoveryMethod('email')
                          sound.tick()
                          setError(null)
                        }}
                        className={`p-3 rounded-2xl border text-left transition flex items-center space-x-2.5 ${
                          recoveryMethod === 'email'
                            ? 'bg-sky-500/15 border-sky-500 text-sky-300 font-bold shadow-md'
                            : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-white'
                        }`}
                      >
                        <Mail className="w-4 h-4" />
                        <div>
                          <div className="text-xs font-semibold">Por Correo</div>
                          <div className="text-[10px] text-slate-400">Mensaje a tu email</div>
                        </div>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setRecoveryMethod('phone')
                          sound.tick()
                          setError(null)
                        }}
                        className={`p-3 rounded-2xl border text-left transition flex items-center space-x-2.5 ${
                          recoveryMethod === 'phone'
                            ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 font-bold shadow-md'
                            : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-white'
                        }`}
                      >
                        <Smartphone className="w-4 h-4" />
                        <div>
                          <div className="text-xs font-semibold">Por Teléfono</div>
                          <div className="text-[10px] text-slate-400">SMS o WhatsApp</div>
                        </div>
                      </button>
                    </div>
                  </div>

                  {/* Input depending on method */}
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">
                      {recoveryMethod === 'email'
                        ? 'Correo Electrónico Registrado'
                        : 'Número de Teléfono Registrado'}
                    </label>
                    <div className="flex gap-2">
                      <div className="relative flex-1">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                          {recoveryMethod === 'email' ? <AtSign className="w-4 h-4" /> : <Phone className="w-4 h-4" />}
                        </div>
                        <input
                          type={recoveryMethod === 'email' ? 'email' : 'tel'}
                          value={recoveryIdentifier}
                          onChange={(e) => setRecoveryIdentifier(e.target.value)}
                          placeholder={
                            recoveryMethod === 'email'
                              ? 'Ej: tu_cuenta@correo.com'
                              : 'Ej: +57 310 123 4567'
                          }
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-sans"
                          required
                        />
                      </div>

                      <button
                        type="button"
                        onClick={handleRequestRecoveryCode}
                        disabled={loading || !recoveryIdentifier.trim()}
                        className="px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-sky-400 hover:text-sky-300 border border-slate-700 text-xs font-bold transition flex items-center space-x-1 shrink-0 disabled:opacity-40"
                      >
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>Generar Código</span>
                      </button>
                    </div>
                  </div>

                  {/* Security PIN verification */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-medium text-slate-300">
                        Código de Seguridad (PIN)
                      </label>
                      {simulatedCode && (
                        <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                          PIN Verificado: {simulatedCode}
                        </span>
                      )}
                    </div>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                        <KeyRound className="w-4 h-4" />
                      </div>
                      <input
                        type="text"
                        value={recoveryPin}
                        onChange={(e) => setRecoveryPin(e.target.value)}
                        placeholder="Ingresa el PIN de 4 dígitos"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono tracking-widest"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1.5">
                        Nueva Contraseña
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                          <Lock className="w-4 h-4" />
                        </div>
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={recoveryNewPassword}
                          onChange={(e) => setRecoveryNewPassword(e.target.value)}
                          placeholder="Mínimo 6 caracteres"
                          className="w-full pl-10 pr-10 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono"
                          required
                          minLength={6}
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1.5">
                        Confirmar Nueva Contraseña
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                          <Lock className="w-4 h-4" />
                        </div>
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={recoveryConfirmPassword}
                          onChange={(e) => setRecoveryConfirmPassword(e.target.value)}
                          placeholder="Repite tu clave"
                          className="w-full pl-10 pr-10 py-2.5 rounded-xl pro-input text-xs sm:text-sm font-mono"
                          required
                          minLength={6}
                        />
                      </div>
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-indigo-600 to-sky-600 hover:from-indigo-500 hover:to-sky-500 text-white font-bold text-xs sm:text-sm tracking-wide shadow-lg shadow-indigo-600/20 transition-all flex items-center justify-center space-x-2 active:scale-[0.99] disabled:opacity-50 cursor-pointer"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <span>Restablecer Contraseña</span>
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </>
              )}
            </form>

            {/* Bottom cancel or switch */}
            <div className="mt-6 pt-5 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-400">
              {mode === 'login' ? (
                <div>
                  ¿No tienes una cuenta aún?{' '}
                  <button
                    type="button"
                    onClick={() => {
                      setMode('register')
                      sound.tick()
                    }}
                    className="text-emerald-400 font-semibold hover:underline"
                  >
                    Crear cuenta gratis
                  </button>
                </div>
              ) : (
                <div>
                  ¿Ya tienes cuenta?{' '}
                  <button
                    type="button"
                    onClick={() => {
                      setMode('login')
                      sound.tick()
                    }}
                    className="text-sky-400 font-semibold hover:underline"
                  >
                    Iniciar sesión aquí
                  </button>
                </div>
              )}

              {onCancel && (
                <button
                  type="button"
                  onClick={onCancel}
                  className="text-slate-500 hover:text-slate-300 transition"
                >
                  Cerrar
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
