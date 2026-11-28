/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['Outfit', 'system-ui', 'sans-serif'],
        display: ['Rajdhani', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      colors: {
        pro: {
          950: '#070a11',
          900: '#0b111e',
          850: '#101828',
          800: '#162238',
          750: '#1e2e4a',
          700: '#263b5c',
          600: '#334e77',
          500: '#47699d',
        },
        nexus: {
          950: '#070a11',
          900: '#0b111e',
          850: '#101828',
          800: '#162238',
          750: '#1e2e4a',
          700: '#263b5c',
          600: '#334e77',
        },
        cyan: {
          glow: '#06b6d4',
          electric: '#0ea5e9',
          accent: '#38bdf8',
        },
        neon: {
          emerald: '#10b981',
          amber: '#f59e0b',
          violet: '#8b5cf6',
          rose: '#f43f5e',
          cyan: '#06b6d4',
        },
        brand: {
          50: '#fff7ed',
          400: '#fb923c',
          500: '#f97316',
          600: '#ea580c',
          700: '#c2410c',
          neon: '#f97316',
        }
      },
      boxShadow: {
        'subtle-card': '0 4px 20px -2px rgba(0, 0, 0, 0.4), 0 0 1px 1px rgba(255, 255, 255, 0.05)',
        'cyan-glow': '0 0 20px -4px rgba(6, 182, 212, 0.3)',
        'cyan-glow-sm': '0 0 10px -2px rgba(6, 182, 212, 0.35)',
        'emerald-glow': '0 0 20px -4px rgba(16, 185, 129, 0.3)',
        'brand-glow': '0 0 20px -4px rgba(249, 115, 22, 0.3)',
        'rose-glow': '0 0 25px -4px rgba(244, 63, 94, 0.4)',
        'glass-panel': '0 12px 40px 0 rgba(0, 0, 0, 0.5)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'radar-sweep': 'radar 4s linear infinite',
        'float-slow': 'float 5s ease-in-out infinite',
      },
      keyframes: {
        radar: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-6px)' },
        }
      }
    },
  },
  plugins: [],
}

