# Auris Backend API Documentation

> For Frontend Team - Complete API Reference

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Base URL & CORS](#base-url--cors)
4. [Data Models](#data-models)
5. [API Endpoints](#api-endpoints)
   - [Auth Routes](#auth-routes)
   - [Patient Routes](#patient-routes)
   - [Check-in Routes](#check-in-routes)
   - [Dashboard Routes](#dashboard-routes)
   - [Therapist Schedule Routes](#therapist-schedule-routes)
   - [System Routes](#system-routes)
6. [Error Handling](#error-handling)
7. [Example Flows](#example-flows)

---

## Overview

**Auris** is a PTSD patient monitoring platform with a FastAPI backend. The API supports:
- **Auth0 JWT authentication** for secure access
- **Role-based access control** (Patient vs Therapist)
- **RESTful endpoints** for all operations
- **Auto-generated docs** via FastAPI

### Auto-Generated Docs (when backend is running)
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`
- **OpenAPI JSON**: `http://localhost:8000/openapi.json`

---

## Authentication

### Auth0 JWT Token

All protected endpoints require an **Auth0 JWT token** in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

### Getting a Token

1. **Frontend (React)**: Use `@auth0/auth0-react` SDK to get tokens
   ```javascript
   const { getAccessTokenSilently } = useAuth0();
   const token = await getAccessTokenSilently();
   ```

2. **Testing**: Get test tokens from Auth0 Dashboard → APIs → Test

### Auth Flow

1. User logs in via Auth0 (on frontend)
2. Frontend gets JWT token from Auth0
3. Include token in all API requests
4. Backend validates token with Auth0

### Auth Configuration Endpoint

Get Auth0 configuration for frontend initialization:

```http
GET /auth/config
```

**Response:**
```json
{
  "domain": "your-tenant.auth0.com",
  "client_id": "your-client-id",
  "audience": "https://auris-api"
}
```

---

## Base URL & CORS

### Development
```
Base URL: http://localhost:8000
```

### Allowed Origins (CORS)
```
http://localhost:3000    # React frontend
http://localhost:8501    # Streamlit dashboard
http://localhost:8000    # Backend itself
```

---

## Data Models

### QuestionResponse
```typescript
interface QuestionResponse {
  question_id: string;      // e.g., "Q1", "Q2"
  response: number;         // 0-3 scale
  hrv_at_question?: number; // HRV captured during this question
}
```

### CheckinSubmit
```typescript
interface CheckinSubmit {
  patient_id: number;
  scale_type: string;       // "PHQ-9" or "GAD-7"
  scale_score: number;      // Total score
  hrv: number;              // Heart rate variability
  breathing_rate: number;
  pulse_rate: number;
  distress: number;         // 1-10 distress rating
  situation: string;        // Situation description
  coping: string;           // Coping strategy
  questions: QuestionResponse[];
}
```

### UserProfileResponse
```typescript
interface UserProfileResponse {
  auth0_user_id: string;
  email: string;
  name: string;
  role: "patient" | "therapist" | "unknown";
  patient_id?: number;
  therapist_id?: number;
  assigned_scale?: string;  // e.g., "PHQ-9" or "GAD-7"
}
```

### Patient
```typescript
interface Patient {
  patient_id: number;
  name: string;
  email: string;
  assigned_scale: string;
  therapist_id: number;
  appointment_day?: string;
  timezone: string;
}
```

### ScaleQuestion
```typescript
interface ScaleQuestion {
  scale_type: string;
  question_id: string;
  question_text: string;
  is_vitals_correlated: boolean;
  clinical_construct?: string;
}
```

### TherapistScheduleInsert
```typescript
interface TherapistScheduleInsert {
  patient_id: number;
  scheduled_dates: string[];  // ISO date strings: ["2024-03-22", "2024-03-29"]
}
```

---

## API Endpoints

### Auth Routes

**Base Path:** `/auth`

#### 1. Get Auth0 Configuration
```http
GET /auth/config
```
**Auth:** None  
**Description:** Get Auth0 configuration for frontend clients.

**Response:**
```json
{
  "domain": "your-tenant.auth0.com",
  "client_id": "your-client-id",
  "audience": "https://auris-api"
}
```

---

#### 2. Get Current User Info
```http
GET /auth/me
```
**Auth:** Required (JWT)  
**Description:** Get current user's full profile from database.

**Response:**
```json
{
  "auth0_user_id": "auth0|123456789",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "patient",
  "patient_id": 1,
  "therapist_id": 1,
  "assigned_scale": "PHQ-9"
}
```

---

#### 3. Sync User (Registration)
```http
POST /auth/sync-user
```
**Auth:** Required (JWT)  
**Description:** Create user record in database after first Auth0 login.

**Request Body:**
```json
{
  "name": "John Doe",
  "role": "patient",
  "therapist_id": 1,
  "assigned_scale": "PHQ-9"
}
```

**Response:** `UserProfileResponse`

---

#### 4. Login
```http
POST /auth/login
```
**Auth:** Required (JWT)  
**Description:** Check if user exists in database or needs registration.

**Response (Existing User):**
```json
{
  "authenticated": true,
  "needs_registration": false,
  "user": {
    "auth0_user_id": "auth0|123",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "patient",
    "patient_id": 1,
    "assigned_scale": "PHQ-9"
  }
}
```

**Response (New User):**
```json
{
  "authenticated": true,
  "needs_registration": true,
  "auth0_user_id": "auth0|123",
  "email": "user@example.com",
  "name": "John Doe",
  "message": "User not found in database. Call /auth/sync-user to register."
}
```

---

#### 5. Public Test (Optional Auth)
```http
GET /auth/public-test
```
**Auth:** Optional  
**Description:** Test endpoint that works with or without authentication.

---

### Patient Routes

**Base Path:** `/patients`

#### 1. Get Patient Info
```http
GET /patients/{patient_id}
```
**Auth:** Required (JWT)  
**Description:** Get patient profile information.

**Response:**
```json
{
  "PATIENT_ID": 1,
  "NAME": "Alex Rivera",
  "EMAIL": "alex@example.com",
  "ASSIGNED_SCALE": "PHQ-9",
  "THERAPIST_ID": 1,
  "APPOINTMENT_DAY": "Monday",
  "TIMEZONE": "America/New_York"
}
```

---

#### 2. Get Patient Questions
```http
GET /patients/get-questions/{patient_id}
```
**Auth:** Required (JWT)  
**Description:** Get all questions for patient based on their assigned scale (PHQ-9 or GAD-7).

**Response:**
```json
{
  "patient_id": 1,
  "patient_name": "Alex Rivera",
  "scale_type": "PHQ-9",
  "questions": [
    {
      "QUESTION_ID": "Q1",
      "QUESTION_TEXT": "Little interest or pleasure in doing things",
      "IS_VITALS_CORRELATED": true,
      "CLINICAL_CONSTRUCT": "Anhedonia"
    }
  ]
}
```

---

#### 3. Get Patient History
```http
GET /patients/{patient_id}/history?limit=10
```
**Auth:** Required (JWT)  
**Query Params:**
- `limit` (optional): Number of records, default 10

**Description:** Get patient's check-in history.

**Response:**
```json
{
  "patient_id": 1,
  "checkins": [
    {
      "SESSION_ID": 1,
      "CHECKIN_DATE": "2024-03-15",
      "SCALE_TYPE": "PHQ-9",
      "SCALE_SCORE": 12,
      "HRV_VALUE": 65.5,
      "BREATHING_RATE": 14.2,
      "PULSE_RATE": 72,
      "DISTRESS_RATING": 6,
      "SITUATION_TEXT": "Work stress...",
      "COPING_TEXT": "Took a walk...",
      "COMPLETED_AT": "2024-03-15T10:30:00"
    }
  ]
}
```

---

#### 4. Get Next Check-in
```http
GET /patients/{patient_id}/next-checkin
```
**Auth:** Required (JWT)  
**Description:** Get next scheduled check-in for patient.

**Response:**
```json
{
  "next_checkin": {
    "SCHEDULE_ID": 1,
    "SCHEDULED_DATE": "2024-03-22"
  },
  "is_checkin_day": true
}
```

---

### Check-in Routes

**Base Path:** `/checkins`

#### 1. Get Scale Questions
```http
GET /checkins/scale-questions/{scale_type}
```
**Auth:** Required (JWT)  
**Path Params:**
- `scale_type`: "PHQ-9" or "GAD-7"

**Description:** Get questions for a specific scale type.

**Response:**
```json
{
  "scale_type": "PHQ-9",
  "questions": [
    {
      "QUESTION_ID": "Q1",
      "QUESTION_TEXT": "Little interest or pleasure in doing things",
      "IS_VITALS_CORRELATED": true,
      "CLINICAL_CONSTRUCT": "Anhedonia"
    }
  ]
}
```

---

#### 2. Submit Check-in
```http
POST /checkins/
```
**Auth:** Required (JWT)  
**Description:** Submit a complete check-in. Embedding pipeline runs in background.

**Request Body:**
```json
{
  "patient_id": 1,
  "scale_type": "PHQ-9",
  "scale_score": 12,
  "hrv": 65.5,
  "breathing_rate": 14.2,
  "pulse_rate": 72,
  "distress": 6,
  "situation": "Had a stressful meeting at work",
  "coping": "Took a 10-minute walk outside",
  "questions": [
    {
      "question_id": "Q1",
      "response": 2,
      "hrv_at_question": 65.5
    },
    {
      "question_id": "Q2",
      "response": 1,
      "hrv_at_question": 62.3
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "session_id": 42,
  "message": "Check-in submitted. Embedding pipeline running in background.",
  "pipeline_status": "processing"
}
```

---

#### 3. Get Check-in Status
```http
GET /checkins/{session_id}/status
```
**Auth:** Required (JWT)  
**Description:** Check if embedding pipeline is complete for a check-in.

**Response:**
```json
{
  "session_id": 42,
  "has_situation_text": true,
  "has_embedding": true,
  "has_cluster": true,
  "cluster_label": "work_stress_cluster",
  "pipeline_complete": true
}
```

---

#### 4. Manual Run Pipeline
```http
POST /checkins/{session_id}/run-pipeline
```
**Auth:** Required (JWT)  
**Description:** Manually trigger embedding pipeline (for retry or admin).

**Response:**
```json
{
  "success": true,
  "session_id": 42,
  "cluster_label": "work_stress_cluster"
}
```

---

### Dashboard Routes

**Base Path:** `/dashboard`

#### 1. Get Therapist's Patients
```http
GET /dashboard/therapists/{therapist_id}/patients
```
**Auth:** Required (JWT)  
**Description:** Get all patients assigned to a therapist with check-in counts.

**Response:**
```json
{
  "patients": [
    {
      "PATIENT_ID": 1,
      "NAME": "Alex Rivera",
      "EMAIL": "alex@example.com",
      "ASSIGNED_SCALE": "PHQ-9",
      "CHECKIN_COUNT": 12
    }
  ]
}
```

---

#### 2. Get Patient Brief
```http
GET /dashboard/patients/{patient_id}/brief
```
**Auth:** Required (JWT)  
**Description:** Get complete patient brief for therapist dashboard (includes trends, vitals, flags, clusters).

**Response:**
```json
{
  "patient_id": 1,
  "latest": {
    "SESSION_ID": 42,
    "CHECKIN_DATE": "2024-03-22",
    "SCALE_SCORE": 12,
    "DISTRESS_RATING": 6
  },
  "weekly_trend": [
    {
      "CHECKIN_DATE": "2024-03-15",
      "SCALE_SCORE": 12,
      "HRV_VALUE": 65.5
    }
  ],
  "question_vitals": [
    {
      "QUESTION_ID": "Q1",
      "QUESTION_TEXT": "Little interest or pleasure...",
      "RESPONSE_VALUE": 2,
      "HRV_AT_QUESTION": 65.5
    }
  ],
  "flags": ["PHQ-9 Q9 elevated - discuss in session"],
  "clusters": ["work_stress_cluster", "social_anxiety_cluster"],
  "cluster_comparisons": []
}
```

---

#### 3. Get Patient Trends
```http
GET /dashboard/patients/{patient_id}/trends
```
**Auth:** Required (JWT)  
**Description:** Get session trends with LAG() calculation for week-over-week comparison.

**Response:**
```json
{
  "patient_id": 1,
  "trends": [
    {
      "session_id": 42,
      "scale_score": 12,
      "prev_scale_score": 14,
      "score_delta": -2
    }
  ]
}
```

---

#### 4. Get Question Trends
```http
GET /dashboard/patients/{patient_id}/question-trends
```
**Auth:** Required (JWT)  
**Description:** Get per-question HRV trends.

---

#### 5. Get Cluster Comparison
```http
GET /dashboard/patients/{patient_id}/cluster/{cluster_label}/comparison
```
**Auth:** Required (JWT)  
**Description:** Get earliest vs latest comparison for a specific cluster.

---

#### 6. Get HRV Sparkline
```http
GET /dashboard/patients/{patient_id}/sparkline/{question_id}
```
**Auth:** Required (JWT)  
**Description:** Get HRV sparkline data for a specific question over time.

**Response:**
```json
{
  "patient_id": 1,
  "question_id": "Q1",
  "readings": [
    {
      "CAPTURED_AT": "2024-03-15T10:30:00",
      "HRV_AT_QUESTION": 65.5,
      "RESPONSE_VALUE": 2
    }
  ]
}
```

---

#### 7. Confirm Cluster Comparison
```http
POST /dashboard/patients/{patient_id}/confirm-cluster
```
**Auth:** Required (JWT)  
**Description:** Therapist confirms cluster comparison direction.

**Request Body:**
```json
{
  "therapist_id": 1,
  "cluster_label": "work_stress_cluster",
  "direction": "better"  // "better", "worse", or "different"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Confirmed better for work_stress_cluster"
}
```

---

### Therapist Schedule Routes

**Base Path:** `/therapists`

#### 1. Insert Schedule
```http
POST /therapists/{therapist_id}/schedule
```
**Auth:** Required (JWT)  
**Description:** Schedule check-in dates for a patient. Patient must belong to this therapist.

**Request Body:**
```json
{
  "patient_id": 1,
  "scheduled_dates": ["2024-03-22", "2024-03-29", "2024-04-05"]
}
```

**Response:**
```json
{
  "success": true,
  "patient_id": 1,
  "therapist_id": 1,
  "inserted": 3,
  "skipped_existing": 0
}
```

---

### System Routes

#### 1. Root/Health Check
```http
GET /
```
**Auth:** None  
**Description:** Basic health check.

**Response:**
```json
{
  "message": "Auris Backend",
  "status": "ok",
  "version": "0.1.0"
}
```

---

#### 2. Detailed Health Check
```http
GET /health
```
**Auth:** None  
**Description:** Database connectivity check.

**Response:**
```json
{
  "status": "healthy",
  "db": "connected"
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET/POST |
| 400 | Bad Request | Invalid request data |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | Valid token but insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Auth0 not configured |

### Error Response Format

```json
{
  "detail": "Error message description"
}
```

### Common Errors

**401 Unauthorized:**
```json
{
  "detail": "Invalid token"
}
```

**404 Not Found:**
```json
{
  "detail": "Patient not found"
}
```

**400 Bad Request:**
```json
{
  "detail": "therapist_id is required for patient registration"
}
```

---

## Example Flows

### Patient Check-in Flow

```javascript
// 1. Get patient questions
const questions = await fetch(`/patients/get-questions/${patientId}`, {
  headers: { Authorization: `Bearer ${token}` }
}).then(r => r.json());

// 2. Submit check-in
const checkinData = {
  patient_id: patientId,
  scale_type: "PHQ-9",
  scale_score: 12,
  hrv: 65.5,
  breathing_rate: 14.2,
  pulse_rate: 72,
  distress: 6,
  situation: "Work stress...",
  coping: "Took a walk...",
  questions: [
    { question_id: "Q1", response: 2, hrv_at_question: 65.5 }
  ]
};

const result = await fetch('/checkins/', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(checkinData)
}).then(r => r.json());

// 3. Poll for pipeline completion
const checkStatus = async (sessionId) => {
  const status = await fetch(`/checkins/${sessionId}/status`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(r => r.json());
  return status.pipeline_complete;
};
```

### User Registration Flow

```javascript
// 1. User logs in with Auth0 (on frontend)
const token = await getAccessTokenSilently();

// 2. Check if user exists
const loginResponse = await fetch('/auth/login', {
  method: 'POST',
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}).then(r => r.json());

// 3. If new user, register
if (loginResponse.needs_registration) {
  await fetch('/auth/sync-user', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: "John Doe",
      role: "patient",
      therapist_id: 1,
      assigned_scale: "PHQ-9"
    })
  });
}
```

### Therapist Dashboard Flow

```javascript
// 1. Get therapist's patients
const patients = await fetch(`/dashboard/therapists/${therapistId}/patients`, {
  headers: { Authorization: `Bearer ${token}` }
}).then(r => r.json());

// 2. Get detailed brief for a patient
const brief = await fetch(`/dashboard/patients/${patientId}/brief`, {
  headers: { Authorization: `Bearer ${token}` }
}).then(r => r.json());

// 3. Get HRV sparkline for a specific question
const sparkline = await fetch(`/dashboard/patients/${patientId}/sparkline/Q1`, {
  headers: { Authorization: `Bearer ${token}` }
}).then(r => r.json());
```

---

## Environment Variables

Backend requires these environment variables:

```env
# Auth0
AUTH0_DOMAIN=your-tenant.auth0.com
AUTH0_API_AUDIENCE=https://auris-api
AUTH0_CLIENT_ID=your-client-id
AUTH0_CLIENT_SECRET=your-client-secret
AUTH0_ALGORITHMS=RS256

# Snowflake
SNOWFLAKE_USER=your-user
SNOWFLAKE_PASSWORD=your-password
SNOWFLAKE_ACCOUNT=your-account
SNOWFLAKE_DATABASE=CADENCE

# App
PROJECT_NAME=Auris
VERSION=0.1.0
DEBUG=true
ALLOWED_HOSTS=["http://localhost:3000","http://localhost:8501"]
```

---

## Additional Resources

- **Auth0 Setup**: See `AUTH0_SETUP.md` for detailed Auth0 configuration
- **Database Schema**: See `backend/snowflake/` for SQL schema files
- **Android Integration**: Auth0 Android SDK setup included in AUTH0_SETUP.md

---

*Last Updated: 2024-03-22*
