# BarboYa - Backend API

BarboYa es el backend de una plataforma de domicilios (tipo DiDi) mobile-first desarrollada con **FastAPI**, **PostgreSQL** y **WebSockets**, lista para ser consumida por la aplicación móvil en **Flutter**.

## Estructura del Repositorio
- `/backend`: FastAPI, SQLAlchemy 2.0 async, Pydantic v2, Alembic, WebSockets.
- `/docker-compose.yml`: Orquestador de contenedores (Postgres y Backend API).
- `/API_CONTRACT.md`: Contrato oficial de API REST y WebSockets.

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
