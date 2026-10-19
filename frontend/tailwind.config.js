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
        nexus: {
          950: '#05070B',
          900: '#07090E',
          850: '#0C1018',
          800: '#111723',
          750: '#171F30',
          700: '#1E293D',
          600: '#2A3852',
        },
        cyan: {
          glow: '#00F2FE',
          electric: '#00D2FF',
          accent: '#38BDF8',
        },
        neon: {
          emerald: '#00E5A3',
          amber: '#F59E0B',
          violet: '#A855F7',
          rose: '#FF3366',
          cyan: '#00F2FE',
        },
        brand: {
          50: '#fff7ed',
          400: '#fb923c',
          500: '#f97316',
          600: '#ea580c',
          700: '#c2410c',
          neon: '#FF6B00',
        }
      },
      boxShadow: {
        'cyan-glow': '0 0 25px -5px rgba(0, 242, 254, 0.35)',
        'cyan-glow-sm': '0 0 12px -2px rgba(0, 242, 254, 0.4)',
        'emerald-glow': '0 0 20px -3px rgba(0, 229, 163, 0.35)',
        'brand-glow': '0 0 25px -4px rgba(249, 115, 22, 0.4)',
        'rose-glow': '0 0 30px -4px rgba(255, 51, 102, 0.5)',
        'glass-panel': '0 8px 32px 0 rgba(0, 0, 0, 0.45)',
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

