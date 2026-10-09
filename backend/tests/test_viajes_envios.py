import pytest
from httpx import AsyncClient
from fastapi import status

@pytest.mark.asyncio
async def test_crear_y_listar_viajes(client: AsyncClient):
    # Registrar y autenticar usuario
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": "viajes_user@barboya.com",
            "password": "Password123*",
            "nombre": "Viajes User",
            "telefono": "3009998877",
            "rol": "CLIENTE"
        }
    )
    login_resp = await client.post(
        "/api/v1/auth/login",
        json={
            "email": "viajes_user@barboya.com",
            "password": "Password123*"
        }
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload = {
        "tipo_servicio": "carro",
        "origen_direccion": "Calle 100 # 15-20, Bogotá",
        "origen_lat": 4.6900,
        "origen_lng": -74.0500,
        "destino_direccion": "Carrera 7 # 72-40, Bogotá",
        "destino_lat": 4.6500,
        "destino_lng": -74.0600,
        "precio_estimado": 18500.0,
        "precio_propuesto": 17000.0
    }
    response = await client.post("/api/v1/viajes/", json=payload, headers=headers)
    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    assert data["precio_estimado"] == 18500.0
    assert data["precio_propuesto"] == 17000.0
    assert data["estado"] == "SOLICITADO"
    assert "codigo_confirmacion" in data

    # Listar viajes
    response_list = await client.get("/api/v1/viajes/", headers=headers)
    assert response_list.status_code == status.HTTP_200_OK
    viajes = response_list.json()
    assert len(viajes) >= 1

@pytest.mark.asyncio
async def test_crear_y_listar_envios(client: AsyncClient):
    # Registrar y autenticar usuario
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": "envios_user@barboya.com",
            "password": "Password123*",
            "nombre": "Envios User",
            "telefono": "3005554433",
            "rol": "CLIENTE"
        }
    )
    login_resp = await client.post(
        "/api/v1/auth/login",
        json={
            "email": "envios_user@barboya.com",
            "password": "Password123*"
        }
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload = {
        "tipo_paquete": "pequeño",
        "descripcion": "Documentos notariales urgentes",
        "peso_kg": 0.5,
        "origen_direccion": "Calle 93 # 12-10, Bogotá",
        "origen_lat": 4.6800,
        "origen_lng": -74.0400,
        "destino_direccion": "Calle 72 # 10-34, Bogotá",
        "destino_lat": 4.6600,
        "destino_lng": -74.0500,
        "nombre_destinatario": "María Pérez",
        "telefono_destinatario": "3101234567",
        "instrucciones": "Entregar en recepción",
        "costo": 12000.0
    }
    response = await client.post("/api/v1/envios/", json=payload, headers=headers)
    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    assert data["descripcion"] == "Documentos notariales urgentes"
    assert data["costo"] == 12000.0
    assert data["estado"] == "SOLICITADO"
    assert "codigo_entrega" in data

    # Listar envios
    response_list = await client.get("/api/v1/envios/", headers=headers)
    assert response_list.status_code == status.HTTP_200_OK
    envios = response_list.json()
    assert len(envios) >= 1
