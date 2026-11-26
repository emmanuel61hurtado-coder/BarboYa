import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Backend Spring Boot. Debe ir antes de '/api'. Conserva el Host para que
      // navegador y API compartan origen (necesario para la cookie de sesión).
      '/api/v1': {
        target: process.env.BARBOYA_API_V1 || 'http://localhost:8081',
        changeOrigin: false
      },
      // Avisos en vivo del backend Spring Boot
      '/ws/v1': {
        target: process.env.BARBOYA_API_V1 || 'http://localhost:8081',
        changeOrigin: false,
        ws: true
      },
      // Backend FastAPI
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
