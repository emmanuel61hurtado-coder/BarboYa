# BarboYa - App Web de Domicilios (Full Stack / Monorepo)

BarboYa es una plataforma de domicilios (tipo DiDi) mobile-first desarrollada con FastAPI (backend), React + Vite + Tailwind (frontend), PostgreSQL y WebSockets.

## Estructura del Repositorio
- `/backend`: FastAPI, SQLAlchemy 2.0 async, Pydantic v2, Alembic, WebSockets.
- `/frontend`: React, Vite, TypeScript, Tailwind, Leaflet, PWA.
- `/docker-compose.yml`: Orquestador de contenedores (Postgres, Backend, Frontend).
- `/API_CONTRACT.md`: Contrato oficial de API y WebSockets.

## Ejecución con Docker Compose
```bash
docker compose up --build
```

## Ejecución local (Backend)
```bash
cd backend
python -m venv venv
source venv/bin/activate  # o venv\Scripts\Activate en Windows
pip install -r requirements.txt
alembic upgrade head
python -m app.scripts.seed
uvicorn app.main:app --reload
```
