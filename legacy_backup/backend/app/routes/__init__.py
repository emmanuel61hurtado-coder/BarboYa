"""
BarboYa API Routes package.
Contains all application endpoints organized by version and module.
"""
from app.routes.v1 import routers as v1_routers
from app.routes.v1 import ws as v1_ws

__all__ = ["v1_routers", "v1_ws"]
