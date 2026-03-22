"""Auth0 JWT authentication utilities."""
import os
from typing import Optional

import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt

security = HTTPBearer()

# Cache for JWKS
_jwks_cache = None


class Auth0Config:
    """Auth0 configuration from environment variables."""
    
    DOMAIN: str = os.getenv("AUTH0_DOMAIN", "")
    API_AUDIENCE: str = os.getenv("AUTH0_API_AUDIENCE", "")
    ALGORITHMS: list = os.getenv("AUTH0_ALGORITHMS", "RS256").split(",")
    
    @classmethod
    def is_configured(cls) -> bool:
        """Check if Auth0 is properly configured."""
        return bool(cls.DOMAIN and cls.API_AUDIENCE)


async def get_jwks() -> dict:
    """Fetch Auth0 JWKS (JSON Web Key Set)."""
    global _jwks_cache
    
    if _jwks_cache is not None:
        return _jwks_cache
    
    if not Auth0Config.DOMAIN:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Auth0 domain not configured"
        )
    
    jwks_url = f"https://{Auth0Config.DOMAIN}/.well-known/jwks.json"
    
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(jwks_url)
            response.raise_for_status()
            _jwks_cache = response.json()
            return _jwks_cache
        except httpx.HTTPError as e:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"Could not fetch Auth0 JWKS: {str(e)}"
            )


def get_rsa_key(jwks: dict, token: str) -> Optional[dict]:
    """Extract RSA key from JWKS that matches the token's kid."""
    try:
        unverified_header = jwt.get_unverified_header(token)
        kid = unverified_header.get("kid")
        
        if not kid:
            return None
        
        for key in jwks.get("keys", []):
            if key.get("kid") == kid:
                return {
                    "kty": key.get("kty"),
                    "kid": key.get("kid"),
                    "use": key.get("use"),
                    "n": key.get("n"),
                    "e": key.get("e"),
                }
        return None
    except JWTError:
        return None


async def verify_token(token: str) -> dict:
    """Verify and decode a JWT token from Auth0."""
    if not Auth0Config.is_configured():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Auth0 not configured"
        )
    
    jwks = await get_jwks()
    rsa_key = get_rsa_key(jwks, token)
    
    if not rsa_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unable to find appropriate key",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    try:
        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=Auth0Config.ALGORITHMS,
            audience=Auth0Config.API_AUDIENCE,
            issuer=f"https://{Auth0Config.DOMAIN}/",
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.JWTClaimsError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid claims (check audience and issuer)",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> dict:
    """FastAPI dependency to get the current authenticated user.
    
    Usage:
        @router.get("/protected")
        async def protected_route(user: dict = Depends(get_current_user)):
            return {"message": f"Hello {user.get('sub')}"}
    """
    token = credentials.credentials
    payload = await verify_token(token)
    return payload


async def get_current_user_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
) -> Optional[dict]:
    """FastAPI dependency to optionally get the current user.
    
    Returns None if no token is provided (for public endpoints with optional auth).
    """
    if not credentials:
        return None
    
    try:
        return await verify_token(credentials.credentials)
    except HTTPException:
        return None
