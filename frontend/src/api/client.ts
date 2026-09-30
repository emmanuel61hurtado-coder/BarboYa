import axios from 'axios';
import { MOCK_COMERCIOS, MOCK_PEDIDOS } from '../mocks/data';

const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true' || true; // default true for prototype

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('barboya_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Mock helpers for prototype fallback
export const mockApi = {
  getComercios: async () => MOCK_COMERCIOS,
  getComercioById: async (id: string) => MOCK_COMERCIOS.find(c => c.id === id),
  getPedidos: async () => MOCK_PEDIDOS,
  createPedido: async (data: any) => {
    const newPed = { id: `ped-${Date.now()}`, ...data, estado: 'CREADO', created_at: new Date().toISOString() };
    MOCK_PEDIDOS.push(newPed);
    return newPed;
  }
};
