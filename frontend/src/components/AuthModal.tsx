import React, { useState, useEffect } from 'react'
import { 
  X, Lock, Phone, User, ShieldCheck, Bike, Car, Store, 
  ArrowRight, KeyRound, AlertCircle, CheckCircle2, Sparkles, Eye, EyeOff, ShieldAlert
} from 'lucide-react'
import { api } from '../utils/api'
import { sound } from '../utils/sound'
import { UserRole, UserSession } from '../types'

interface AuthModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (session: UserSession) => void
  initialRole?: UserRole
}

type SelectableRole = 'customer' | 'driver' | 'merchant_owner' | 'admin'

const ROLE_CONFIG: Record<SelectableRole, {
  label: string
  subtitle: string
  icon: any
  activeBorder: string
  activeBg: string
  textColor: string
  badgeBg: string
}> = {
  customer: {
    label: 'Cliente',
    subtitle: 'Viajes, comida y envíos',
    icon: User,
    activeBorder: 'border-cyan-500',
    activeBg: 'bg-cyan-500/20',
    textColor: 'text-cyan-300',
    badgeBg: 'bg-cyan-500/10 border-cyan-500/30 text-cyan-300',
  },
  driver: {
    label: 'Conductor',
    subtitle: 'Centro operativo y viajes',
    icon: Bike,
    activeBorder: 'border-neon-emerald',
    activeBg: 'bg-neon-emerald/20',
    textColor: 'text-neon-emerald',
    badgeBg: 'bg-neon-emerald/10 border-neon-emerald/30 text-neon-emerald',
  },
  merchant_owner: {
    label: 'Comercio',
    subtitle: 'Restaurante y pedidos',
    icon: Store,
    activeBorder: 'border-brand-500',
    activeBg: 'bg-brand-500/20',
    textColor: 'text-brand-300',
    badgeBg: 'bg-brand-500/10 border-brand-500/30 text-brand-300',
  },
  admin: {
    label: 'Administrador',
    subtitle: 'Control y auditoría',
    icon: ShieldCheck,
    activeBorder: 'border-purple-500',
    activeBg: 'bg-purple-500/20',
    textColor: 'text-purple-300',
    badgeBg: 'bg-purple-500/10 border-purple-500/30 text-purple-300',
  },
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  initialRole = 'customer',
}) => {
  const [mode, setMode] = useState<'login' | 'register' | 'recover'>('login')
  const [selectedRole, setSelectedRole] = useState<SelectableRole>(
    initialRole === 'admin' || initialRole === 'superadmin'
      ? 'admin'
      : initialRole === 'driver' || initialRole === 'courier'
      ? 'driver'
      : initialRole === 'merchant_owner' || initialRole === 'merchant_employee'
      ? 'merchant_owner'
      : 'customer'
  )

  useEffect(() => {
    if (initialRole) {
      if (initialRole === 'admin' || initialRole === 'superadmin') setSelectedRole('admin')
      else if (initialRole === 'driver' || initialRole === 'courier') setSelectedRole('driver')
      else if (initialRole === 'merchant_owner' || initialRole === 'merchant_employee') setSelectedRole('merchant_owner')
      else setSelectedRole('customer')
    }
  }, [initialRole, isOpen])

  // Form states
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  // Driver extra fields
  const [vehicleType, setVehicleType] = useState<'moto' | 'car'>('moto')
  const [vehiclePlate, setVehiclePlate] = useState('')
  const [documentId, setDocumentId] = useState('')

  // Merchant extra fields
  const [storeName, setStoreName] = useState('')
  const [storeCategory, setStoreCategory] = useState('restaurant')
  const [storeAddress, setStoreAddress] = useState('')

  // Status
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  if (!isOpen) return null

  const activeConfig = ROLE_CONFIG[selectedRole]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccessMsg(null)
    setLoading(true)

    try {
      if (mode === 'login') {
        // Enviar expected_role para validación estricta de inicio de sesión por roles
        const res = await api.login(phone, password, selectedRole)
        sound.success()
        const me = await api.getMe()
        const session: UserSession = {
          token: res.access_token,
          role: res.role as UserRole,
          userId: res.user_id,
          fullName: me.full_name,
          phone: me.phone,
          avatarUrl: me.avatar_url,
          gender: me.gender,
          driverInfo: me.driver_info,
          merchantInfo: me.merchant_info,
        }
        localStorage.setItem('barboya_user_session', JSON.stringify(session))
        onSuccess(session)
        onClose()
      } else if (mode === 'register') {
        if (selectedRole === 'admin') {
          throw new Error('El rol de administrador no admite auto-registro. Es asignado por el sistema.')
        }

        const res = await api.register({
          fullName,
          phone,
          password,
          role: selectedRole,
          vehicleType: selectedRole === 'driver' ? vehicleType : undefined,
          vehiclePlate: selectedRole === 'driver' ? vehiclePlate : undefined,
          documentId: selectedRole === 'driver' ? documentId : undefined,
        })

        if (selectedRole === 'merchant_owner') {
          try {
            await api.registerMerchant({
              name: storeName || `Comercio de ${fullName}`,
              category: storeCategory,
              addressNote: storeAddress || 'Barbosa Centro',
              location: { lat: 5.9328, lng: -73.6204 },
              phone,
            })
          } catch (mErr) {
            console.warn('Error al auto-registrar tienda:', mErr)
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
          driverInfo: me.driver_info,
          merchantInfo: me.merchant_info,
        }
        localStorage.setItem('barboya_user_session', JSON.stringify(session))
        onSuccess(session)
        onClose()
      } else if (mode === 'recover') {
        await api.recoverPassword(phone, newPassword)
        sound.success()
        setSuccessMsg('Contraseña actualizada con éxito. Ya puedes iniciar sesión con tu rol.')
        setMode('login')
      }
    } catch (err: any) {
      sound.alert()
      setError(err.message || 'Error en la autenticación. Verifica tus credenciales o rol seleccionado.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-xl animate-fade-in">
      <div className="relative w-full max-w-lg glass-surface-elevated rounded-2xl border border-cyan-500/30 p-6 md:p-8 shadow-cyan-glow max-h-[92vh] overflow-y-auto">
        {/* Close Button */}
        <button
          onClick={() => {
            sound.tick()
            onClose()
          }}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-xl bg-nexus-900/60 border border-slate-800 transition"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="text-center mb-5">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/30 text-cyan-300 text-xs font-mono mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>ACCESO POR ROLES · BARBOYA NEXUS</span>
          </div>
          <h2 className="text-2xl font-black text-white tracking-wide">
            {mode === 'login' && 'INICIAR SESIÓN'}
            {mode === 'register' && 'CREAR CUENTA'}
            {mode === 'recover' && 'RECUPERAR ACCESO'}
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            {mode === 'login' && 'Selecciona tu rol para ingresar directamente a tu centro de operaciones'}
            {mode === 'register' && 'Elige tu tipo de cuenta para unirte al ecosistema de Barbosa'}
            {mode === 'recover' && 'Ingresa tu número registrado para restablecer tu contraseña'}
          </p>
        </div>

        {/* Role Selector (Prominente en Login y Register) */}
        {mode !== 'recover' && (
          <div className="mb-5">
            <label className="block text-[11px] font-mono text-slate-400 mb-2">
              SELECCIONA TU ROL DE ACCESO:
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(['customer', 'driver', 'merchant_owner', 'admin'] as SelectableRole[]).map((r) => {
                const conf = ROLE_CONFIG[r]
                const IconComponent = conf.icon
                const isSelected = selectedRole === r
                const isRegisterDisabled = mode === 'register' && r === 'admin'

                return (
                  <button
                    key={r}
                    type="button"
                    disabled={isRegisterDisabled}
                    onClick={() => {
                      if (!isRegisterDisabled) {
                        setSelectedRole(r)
                        sound.tick()
                        setError(null)
                      }
                    }}
                    className={`p-2.5 rounded-xl border text-center transition flex flex-col items-center space-y-1 relative ${
                      isRegisterDisabled
                        ? 'opacity-30 cursor-not-allowed bg-nexus-950/40 border-slate-900'
                        : isSelected
                        ? `${conf.activeBg} ${conf.activeBorder} ${conf.textColor} font-bold shadow-md`
                        : 'bg-nexus-900/60 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                    }`}
                  >
                    <IconComponent className="w-4 h-4" />
                    <span className="text-xs">{conf.label}</span>
                    <span className="text-[9px] font-mono opacity-60 truncate w-full">
                      {r === 'customer' ? 'Pasajero' : r === 'driver' ? 'Operador' : r === 'merchant_owner' ? 'Tienda' : 'Sistema'}
                    </span>
                  </button>
                )
              })}
            </div>

            {/* Hint of selected role */}
            <div className="mt-2.5 p-2 rounded-xl bg-nexus-950 border border-slate-800/80 flex items-center justify-between text-xs">
              <span className="text-slate-400">Accediendo como:</span>
              <span className={`font-mono font-bold px-2 py-0.5 rounded text-[11px] ${activeConfig.badgeBg}`}>
                {activeConfig.label.toUpperCase()} · {activeConfig.subtitle}
              </span>
            </div>
          </div>
        )}

        {/* Alerts */}
        {error && (
          <div className="mb-4 p-3 rounded-xl bg-tactical-pink/10 border border-tactical-pink/30 flex items-start space-x-2 text-xs text-tactical-pink animate-shake">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="mb-4 p-3 rounded-xl bg-neon-emerald/10 border border-neon-emerald/30 flex items-start space-x-2 text-xs text-neon-emerald">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'register' && (
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Nombre Completo</label>
              <div className="relative">
                <User className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Ej: Carlos Silva"
                  className="w-full bg-nexus-950/80 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-400 transition"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Teléfono Registrado ({activeConfig.label})
            </label>
            <div className="relative">
              <Phone className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
              <input
                type="tel"
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+573101234567"
                className="w-full bg-nexus-950/80 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-400 transition"
              />
            </div>
          </div>

          {mode !== 'recover' && (
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Contraseña</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-nexus-950/80 border border-slate-800 rounded-xl pl-9 pr-10 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-400 transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-slate-500 hover:text-slate-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          )}

          {mode === 'recover' && (
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Nueva Contraseña</label>
              <div className="relative">
                <KeyRound className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
                <input
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Mínimo 6 caracteres"
                  className="w-full bg-nexus-950/80 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-400 transition"
                />
              </div>
            </div>
          )}

          {/* Conditional Driver Fields */}
          {mode === 'register' && selectedRole === 'driver' && (
            <div className="p-3.5 rounded-xl bg-neon-emerald/5 border border-neon-emerald/20 space-y-3">
              <div className="flex items-center space-x-2 text-xs text-neon-emerald font-mono">
                <Bike className="w-4 h-4" />
                <span>DATOS OPERATIVOS DEL VEHÍCULO</span>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setVehicleType('moto')}
                  className={`p-2 rounded-lg border text-xs flex items-center justify-center space-x-1 ${
                    vehicleType === 'moto'
                      ? 'bg-neon-emerald/20 border-neon-emerald text-neon-emerald font-bold'
                      : 'border-slate-800 text-slate-400'
                  }`}
                >
                  <Bike className="w-3.5 h-3.5" />
                  <span>Motocicleta</span>
                </button>
                <button
                  type="button"
                  onClick={() => setVehicleType('car')}
                  className={`p-2 rounded-lg border text-xs flex items-center justify-center space-x-1 ${
                    vehicleType === 'car'
                      ? 'bg-neon-emerald/20 border-neon-emerald text-neon-emerald font-bold'
                      : 'border-slate-800 text-slate-400'
                  }`}
                >
                  <Car className="w-3.5 h-3.5" />
                  <span>Automóvil</span>
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-mono text-slate-400 mb-1">PLACA</label>
                  <input
                    type="text"
                    required
                    value={vehiclePlate}
                    onChange={(e) => setVehiclePlate(e.target.value.toUpperCase())}
                    placeholder="XYZ-12D"
                    className="w-full bg-nexus-950 border border-slate-800 rounded-lg p-2 text-xs text-white uppercase font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-mono text-slate-400 mb-1">CÉDULA / DOC</label>
                  <input
                    type="text"
                    required
                    value={documentId}
                    onChange={(e) => setDocumentId(e.target.value)}
                    placeholder="1098..."
                    className="w-full bg-nexus-950 border border-slate-800 rounded-lg p-2 text-xs text-white font-mono"
                  />
                </div>
              </div>
              <p className="text-[10px] text-slate-400">
                🔒 Por seguridad, tu cuenta requerirá validación de SOAT y Licencia antes de recibir solicitudes.
              </p>
            </div>
          )}

          {/* Conditional Merchant Fields */}
          {mode === 'register' && selectedRole === 'merchant_owner' && (
            <div className="p-3.5 rounded-xl bg-brand-500/5 border border-brand-500/20 space-y-3">
              <div className="flex items-center space-x-2 text-xs text-brand-400 font-mono">
                <Store className="w-4 h-4" />
                <span>INFORMACIÓN DEL ESTABLECIMIENTO</span>
              </div>
              <div>
                <label className="block text-[10px] font-mono text-slate-400 mb-1">NOMBRE COMERCIAL</label>
                <input
                  type="text"
                  required
                  value={storeName}
                  onChange={(e) => setStoreName(e.target.value)}
                  placeholder="Ej: Pizzería El Puente"
                  className="w-full bg-nexus-950 border border-slate-800 rounded-lg p-2 text-xs text-white"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-mono text-slate-400 mb-1">CATEGORÍA</label>
                  <select
                    value={storeCategory}
                    onChange={(e) => setStoreCategory(e.target.value)}
                    className="w-full bg-nexus-950 border border-slate-800 rounded-lg p-2 text-xs text-white"
                  >
                    <option value="restaurant">Restaurante</option>
                    <option value="bakery">Panadería</option>
                    <option value="grocery">Víveres / Super</option>
                    <option value="pharmacy">Droguería</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[10px] font-mono text-slate-400 mb-1">DIRECCIÓN / REF</label>
                  <input
                    type="text"
                    required
                    value={storeAddress}
                    onChange={(e) => setStoreAddress(e.target.value)}
                    placeholder="Cra 8 # 7-20"
                    className="w-full bg-nexus-950 border border-slate-800 rounded-lg p-2 text-xs text-white"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Action Button */}
          <button
            type="submit"
            disabled={loading}
            className={`w-full py-3.5 rounded-xl font-black text-xs sm:text-sm tracking-wider uppercase flex items-center justify-center space-x-2 shadow-lg hover:opacity-90 active:scale-[0.99] transition disabled:opacity-50 text-nexus-950 ${
              selectedRole === 'customer'
                ? 'bg-gradient-to-r from-cyan-400 via-cyan-300 to-cyan-500 shadow-cyan-glow'
                : selectedRole === 'driver'
                ? 'bg-gradient-to-r from-neon-emerald via-emerald-400 to-teal-500 shadow-emerald-glow'
                : selectedRole === 'merchant_owner'
                ? 'bg-gradient-to-r from-brand-500 via-orange-400 to-amber-500 shadow-brand-glow text-white'
                : 'bg-gradient-to-r from-purple-500 via-purple-400 to-indigo-500 shadow-purple-glow text-white'
            }`}
          >
            {loading ? (
              <span className="font-mono">VALIDANDO CREDENCIALES...</span>
            ) : (
              <>
                <span>
                  {mode === 'login' && `INGRESAR COMO ${activeConfig.label.toUpperCase()}`}
                  {mode === 'register' && `REGISTRARME COMO ${activeConfig.label.toUpperCase()}`}
                  {mode === 'recover' && 'ACTUALIZAR CONTRASEÑA'}
                </span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        {/* Footer Navigation */}
        <div className="mt-5 pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          {mode === 'login' ? (
            <>
              <button
                type="button"
                onClick={() => {
                  setMode('register')
                  sound.tick()
                  setError(null)
                }}
                className="hover:text-cyan-400 transition"
              >
                ¿No tienes cuenta? <strong className="text-white">Regístrate</strong>
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('recover')
                  sound.tick()
                  setError(null)
                }}
                className="text-slate-500 hover:text-slate-300 transition"
              >
                ¿Olvidaste tu contraseña?
              </button>
            </>
          ) : (
            <div className="w-full text-center">
              <button
                type="button"
                onClick={() => {
                  setMode('login')
                  sound.tick()
                  setError(null)
                }}
                className="hover:text-cyan-400 transition"
              >
                ¿Ya tienes cuenta? <strong className="text-white">Inicia Sesión por Rol</strong>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
