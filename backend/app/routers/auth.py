"""Authentication related routes."""
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from app.core.auth import get_current_user, get_current_user_optional, Auth0Config

router = APIRouter(prefix="/auth", tags=["authentication"])
security = HTTPBearer()


@router.get("/config")
async def get_auth_config():
    """Get Auth0 configuration for frontend clients.
    
    Returns the public Auth0 configuration needed to initialize
    Auth0 SDK in frontend applications.
    """
    if not Auth0Config.is_configured():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Auth0 not configured on server"
        )
    
    return {
        "domain": Auth0Config.DOMAIN,
        "client_id": Auth0Config.CLIENT_ID,
        "audience": Auth0Config.API_AUDIENCE,
    }


@router.get("/me")
async def get_current_user_info(user: dict = Depends(get_current_user)):
    """Get current authenticated user details from JWT token.
    
    Requires a valid Auth0 access token in the Authorization header.
    """
    return {
        "user_id": user.get("sub"),
        "email": user.get("email"),
        "email_verified": user.get("email_verified"),
        "name": user.get("name"),
        "nickname": user.get("nickname"),
        "picture": user.get("picture"),
        "permissions": user.get("permissions", []),
        "roles": user.get(f"{Auth0Config.API_AUDIENCE}/roles", []),
    }


@router.get("/test", include_in_schema=False)
async def test_auth(user: dict = Depends(get_current_user)):
    """Test endpoint to verify authentication is working."""
    return {
        "message": "Authentication successful!",
        "user_id": user.get("sub"),
        "token_claims": user
    }


@router.get("/public-test")
async def public_test(user: dict = Depends(get_current_user_optional)):
    """Public endpoint that also accepts optional authentication."""
    if user:
        return {
            "message": "Hello authenticated user!",
            "user_id": user.get("sub"),
            "authenticated": True
        }
    return {
        "message": "Hello anonymous user!",
        "authenticated": False
    }
