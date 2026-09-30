"""
CRUD Tests: Categorias
Tests para:
  GET /categorias  - listar categorías (público)
"""
import pytest


@pytest.mark.asyncio
async def test_list_categorias_returns_list(client):
    """El endpoint es público y devuelve una lista (puede estar vacía)."""
    res = await client.get("/api/v1/categorias")
    assert res.status_code == 200
    assert isinstance(res.json(), list)
