"""Authentication related routes."""
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from typing import Optional, Literal

from app.core.auth import get_current_user, get_current_user_optional, Auth0Config
from app.core.database import get_db, SnowflakeDB

security = HTTPBearer()

router = APIRouter(
    prefix="/auth",
    tags=["authentication"],
    # dependencies=[Depends(security)]
)

class UserProfileResponse(BaseModel):
    """User profile response."""
    auth0_user_id: str
    email: str
    name: str
    role: Literal["patient", "therapist", "unknown"]
    patient_id: Optional[int] = None
    therapist_id: Optional[int] = None
    assigned_scale: Optional[str] = None


class SyncUserRequest(BaseModel):
    """Request to sync user after Auth0 login."""
    name: Optional[str] = None
    role: Optional[Literal["patient", "therapist"]] = None
    therapist_id: Optional[int] = None  # For new patients: assign to which therapist
    assigned_scale: Optional[str] = "PHQ-9"  # For new patients: PHQ-9 or GAD-7


@router.get("/config")
async def get_auth_config():
    """Get Auth0 configuration for frontend clients."""
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


@router.get("/me", response_model=UserProfileResponse)
async def get_current_user_info(
    # user: dict = Depends(get_current_user),
    db: SnowflakeDB = Depends(get_db)
):
    """Get current user's full profile from our database.
    
    This checks both PATIENTS and THERAPISTS tables to find the user.
    """
    user = {}  # TODO: restore Depends(get_current_user)
    auth0_user_id = user.get("sub")
    email = user.get("email", "")
    name = user.get("name", "")
    
    # Try to find as patient
    patient_result = await db.query("""
        SELECT PATIENT_ID, NAME, ASSIGNED_SCALE, THERAPIST_ID
        FROM CADENCE.PUBLIC.PATIENTS
        WHERE AUTH0_USER_ID = %s
    """, (auth0_user_id,))
    
    if patient_result:
        p = patient_result[0]
        return UserProfileResponse(
            auth0_user_id=auth0_user_id,
            email=email,
            name=p["NAME"] or name,
            role="patient",
            patient_id=p["PATIENT_ID"],
            therapist_id=p["THERAPIST_ID"],
            assigned_scale=p["ASSIGNED_SCALE"]
        )
    
    # Try to find as therapist
    therapist_result = await db.query("""
        SELECT THERAPIST_ID, NAME
        FROM CADENCE.PUBLIC.THERAPISTS
        WHERE AUTH0_USER_ID = %s
    """, (auth0_user_id,))
    
    if therapist_result:
        t = therapist_result[0]
        return UserProfileResponse(
            auth0_user_id=auth0_user_id,
            email=email,
            name=t["NAME"] or name,
            role="therapist",
            therapist_id=t["THERAPIST_ID"]
        )
    
    # User not found in our database - needs registration
    return UserProfileResponse(
        auth0_user_id=auth0_user_id,
        email=email,
        name=name,
        role="unknown"
    )


@router.post("/sync-user", response_model=UserProfileResponse)
async def sync_user(
    data: SyncUserRequest,
    # user: dict = Depends(get_current_user),
    db: SnowflakeDB = Depends(get_db)
):
    """Sync Auth0 user to our database after first login.
    
    Call this after user logs in with Auth0 to create their record in our DB.
    """
    user = {}  # TODO: restore Depends(get_current_user)
    auth0_user_id = user.get("sub")
    email = user.get("email", "")
    name = data.name or user.get("name", "")
    
    # Check if already exists
    existing = await db.query("""
        SELECT 'patient' as role, PATIENT_ID as id FROM CADENCE.PUBLIC.PATIENTS WHERE AUTH0_USER_ID = %s
        UNION ALL
        SELECT 'therapist' as role, THERAPIST_ID as id FROM CADENCE.PUBLIC.THERAPISTS WHERE AUTH0_USER_ID = %s
    """, (auth0_user_id, auth0_user_id))
    
    if existing:
        raise HTTPException(
            status_code=400,
            detail="User already exists in database. Use /auth/me instead."
        )
    
    # Create new user based on role
    if data.role == "therapist":
        await db.execute("""
            INSERT INTO CADENCE.PUBLIC.THERAPISTS (AUTH0_USER_ID, NAME, EMAIL)
            VALUES (%s, %s, %s)
        """, (auth0_user_id, name, email))
        
        # Get the new therapist_id
        result = await db.query("""
            SELECT THERAPIST_ID FROM CADENCE.PUBLIC.THERAPISTS WHERE AUTH0_USER_ID = %s
        """, (auth0_user_id,))
        
        return UserProfileResponse(
            auth0_user_id=auth0_user_id,
            email=email,
            name=name,
            role="therapist",
            therapist_id=result[0]["THERAPIST_ID"]
        )
    
    elif data.role == "patient":
        if not data.therapist_id:
            raise HTTPException(
                status_code=400,
                detail="therapist_id is required for patient registration"
            )
        
        await db.execute("""
            INSERT INTO CADENCE.PUBLIC.PATIENTS 
            (AUTH0_USER_ID, NAME, EMAIL, THERAPIST_ID, ASSIGNED_SCALE, APPOINTMENT_DAY, TIMEZONE)
            VALUES (%s, %s, %s, %s, %s, 'Monday', 'America/New_York')
        """, (auth0_user_id, name, email, data.therapist_id, data.assigned_scale))
        
        # Get the new patient_id
        result = await db.query("""
            SELECT PATIENT_ID, ASSIGNED_SCALE, THERAPIST_ID 
            FROM CADENCE.PUBLIC.PATIENTS WHERE AUTH0_USER_ID = %s
        """, (auth0_user_id,))
        
        return UserProfileResponse(
            auth0_user_id=auth0_user_id,
            email=email,
            name=name,
            role="patient",
            patient_id=result[0]["PATIENT_ID"],
            therapist_id=result[0]["THERAPIST_ID"],
            assigned_scale=result[0]["ASSIGNED_SCALE"]
        )
    
    else:
        raise HTTPException(
            status_code=400,
            detail="role must be 'patient' or 'therapist'"
        )


@router.post("/login")
async def login(
    # user: dict = Depends(get_current_user),
    db: SnowflakeDB = Depends(get_db)
):
    """Login endpoint - returns user profile if exists, or indicates need for registration.
    
    Frontend flow:
    1. User logs in with Auth0 (on frontend)
    2. Frontend sends token to this endpoint
    3. If user exists -> return profile
    4. If user new -> return {needs_registration: true} 
       -> Frontend calls /auth/sync-user to create account
    """
    user = {}  # TODO: restore Depends(get_current_user)
    auth0_user_id = user.get("sub")
    email = user.get("email", "")
    name = user.get("name", "")
    
    # Check if user exists as patient
    patient_result = await db.query("""
        SELECT PATIENT_ID, NAME, ASSIGNED_SCALE, THERAPIST_ID
        FROM CADENCE.PUBLIC.PATIENTS
        WHERE AUTH0_USER_ID = %s
    """, (auth0_user_id,))
    
    if patient_result:
        p = patient_result[0]
        return {
            "authenticated": True,
            "needs_registration": False,
            "user": {
                "auth0_user_id": auth0_user_id,
                "email": email,
                "name": p["NAME"],
                "role": "patient",
                "patient_id": p["PATIENT_ID"],
                "therapist_id": p["THERAPIST_ID"],
                "assigned_scale": p["ASSIGNED_SCALE"]
            }
        }
    
    # Check if user exists as therapist
    therapist_result = await db.query("""
        SELECT THERAPIST_ID, NAME
        FROM CADENCE.PUBLIC.THERAPISTS
        WHERE AUTH0_USER_ID = %s
    """, (auth0_user_id,))
    
    if therapist_result:
        t = therapist_result[0]
        return {
            "authenticated": True,
            "needs_registration": False,
            "user": {
                "auth0_user_id": auth0_user_id,
                "email": email,
                "name": t["NAME"],
                "role": "therapist",
                "therapist_id": t["THERAPIST_ID"]
            }
        }
    
    # New user - needs registration
    return {
        "authenticated": True,
        "needs_registration": True,
        "auth0_user_id": auth0_user_id,
        "email": email,
        "name": name,
        "message": "User not found in database. Call /auth/sync-user to register."
    }


@router.get("/public-test")
async def public_test(
    # user: dict = Depends(get_current_user_optional)
):
    """Public endpoint that also accepts optional authentication."""
    user = None  # TODO: restore Depends(get_current_user_optional)
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
