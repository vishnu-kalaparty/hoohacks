"""
Auris Backend - MVP (Async + Dependency Injection + Auth0)
"""

from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import PROJECT_NAME, VERSION, ALLOWED_HOSTS, AUTH0_CONFIG
from app.core.auth import get_current_user
from app.routers import patients, checkins, dashboard, auth

app = FastAPI(
    title=PROJECT_NAME,
    version=VERSION,
    description="Auris API with Auth0 authentication"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_HOSTS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(patients.router)
app.include_router(checkins.router)
app.include_router(dashboard.router)
app.include_router(auth.router)


@app.get("/")
async def root():
    return {"message": f"{PROJECT_NAME} Backend", "status": "ok", "version": VERSION}


@app.get("/health")
async def health():
    from app.core.database import get_db
    try:
        db = get_db()
        await db.query("SELECT 1")
        return {"status": "healthy", "db": "connected"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}


# @app.get("/auth/config")
# async def auth_config():
#     """Get Auth0 configuration for frontend clients."""
#     return {
#         "domain": AUTH0_CONFIG["domain"],
#         "client_id": AUTH0_CONFIG["client_id"],
#         "audience": AUTH0_CONFIG["api_audience"],
#     }


# @app.get("/protected")
# async def protected_route(user: dict = Depends(get_current_user)):
#     """Example protected route - requires valid Auth0 JWT token."""
#     return {
#         "message": "This is a protected route",
#         "user_id": user.get("sub"),
#         "email": user.get("email"),
#         "permissions": user.get("permissions", [])
#     }


# @app.get("/me")
# async def get_me(user: dict = Depends(get_current_user)):
#     """Get current user info from Auth0 token."""
#     return {
#         "user_id": user.get("sub"),
#         "email": user.get("email"),
#         "name": user.get("name"),
#         "picture": user.get("picture"),
#         "roles": user.get("https://auris.io/roles", []),
#     }


# Run: uvicorn main:app --reload
