import React, { Suspense, lazy } from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// `v1` conecta con el backend Spring Boot; sin esa variable se mantiene la app actual
const Root = import.meta.env.VITE_API_MODE === 'v1' ? lazy(() => import('./routes/PlatformApp')) : App

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Suspense fallback={null}>
      <Root />
    </Suspense>
  </React.StrictMode>,
)
