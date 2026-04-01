# Auris: Between Session Intelligence

A full-stack system for PTSD patient monitoring between therapy sessions.
**Authentication is handled with [Auth0](https://auth0.com)** (JWT validation on
the API; SPA and native clients use the Auth0 SDK).

## Overview

**Auris** helps therapists monitor patient well-being between sessions through:

- Regular check-ins with clinical scales (PHQ-9, GAD-7)
- Vitals-aware screening flows
- Embeddings and similarity tooling for session insights (see backend services)
- Snowflake-backed storage with optional Cortex-related SQL in the repo

## What’s in this repository

| Area            | Description                                                                                                             |
| --------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **Backend**     | FastAPI app with **Auth0**-protected routes, Snowflake access, audit middleware                                         |
| **Android app** | Kotlin client (`android_app/`) for screening, therapist flows, and API calls with bearer tokens                         |
| **Docs**        | [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md), [`AUTH0_SETUP.md`](AUTH0_SETUP.md), root [`.env.example`](.env.example) |

There is no React or Streamlit app in this tree; the
[`.env.example`](.env.example) still lists typical local URLs if you add a web
frontend later.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AURIS PLATFORM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐                                                           │
│  │   Presage    │                                                           │
│  │ SmartSpectra │  (camera / vitals SDK on device)                           │
│  └──────▲───────┘                                                           │
│  vitals │                                                                   │
│  ┌──────┴───────┐         ┌──────────────┐         ┌──────────────┐           │
│  │   Android    │  HTTPS  │   FastAPI    │         │  Snowflake   │           │
│  │   (Kotlin)   │◄───────►│   Backend    │◄───────►│  (Database)  │           │
│  │   Auris app  │         │  + Auth0 JWT │         │              │           │
│  └──────┬───────┘         └──────┬───────┘         └──────────────┘           │
│         │                        │                                           │
│         └────────────────────────┼──────────────── Auth0 (login / tokens)      │
│                                  │                                           │
└──────────────────────────────────┴──────────────────────────────────────────┘
```

Optional integrations (configured via env when used): Twilio (SMS)

## Tech stack

| Layer  | Technology                                                     |
| ------ | -------------------------------------------------------------- |
| API    | FastAPI (Python), **Auth0** (JWT + `/auth/config` for clients) |
| Data   | Snowflake                                                      |
| Mobile | Android (Kotlin), Retrofit                                     |
| Docs   | FastAPI OpenAPI (`/docs`, `/redoc`)                            |

## Project structure

```
hoohacks/
├── android_app/              # Android client (package com.example.cadence, app name "Auris")
├── backend/
│   ├── main.py               # FastAPI entrypoint (uvicorn main:app)
│   ├── requirements.txt
│   ├── app/
│   │   ├── core/             # Config, Auth0, database
│   │   ├── middleware/       # Audit logging
│   │   ├── routers/          # auth, patients, checkins, dashboard, therapists
│   │   ├── services/         # e.g. embedding pipeline
│   │   └── models/
│   └── snowflake/            # SQL scripts (e.g. mock data, audit log)
├── bridge_api/               # Placeholder (reserved)
├── API_DOCUMENTATION.md      # Endpoint reference for API consumers
├── AUTH0_SETUP.md            # Auth0 tenant, API, and env walkthrough
├── .env.example              # Shared env template (Auth0, Snowflake, optional Twilio/SmartSpectra)
└── README.md
```

## Prerequisites

- Python 3.9+
- Android Studio (for the mobile app)
- Snowflake account and credentials
- **Auth0** account (application + API as in [`AUTH0_SETUP.md`](AUTH0_SETUP.md))
- Optional: Twilio, SmartSpectra SDK access (if you enable those features)

## Quick start

### 1. Clone

```bash
git clone <repository-url>
cd hoohacks
```

### 2. Environment

Copy and fill values (Auth0 and Snowflake are required for a full run):

```bash
cp .env.example .env
```

For **Auth0**, follow [`AUTH0_SETUP.md`](AUTH0_SETUP.md). Typical backend
variables:

- `AUTH0_DOMAIN`, `AUTH0_API_AUDIENCE`, `AUTH0_CLIENT_ID`,
  `AUTH0_CLIENT_SECRET`, `AUTH0_ALGORITHMS`

The backend loads configuration from the environment (see
`backend/app/core/config.py`). Point `ALLOWED_HOSTS` / origins at any web or
tunnel URL you use.

### 3. Backend

```bash
cd backend
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
# Ensure env vars are set (e.g. export from repo root .env or use your own method)
uvicorn main:app --reload
```

- API base: `http://localhost:8000`
- **Swagger**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

Auth helper for clients: `GET /auth/config` returns Auth0 `domain`, `client_id`,
and `audience` when the server is configured.

### 4. Snowflake

Run SQL under `backend/snowflake/` in your warehouse (order and full set depend
on your deployment; the repo includes scripts such as `04_mock_data.sql` and
`05_audit_log.sql`). Align database/schema names with your `.env`.

### 5. Android app

Open `android_app/` in Android Studio. Configure the API base URL where your
backend is reachable (see `RetrofitClient`); pass a **Bearer** token from Auth0
after login for protected calls.

## API documentation

- Generated docs: `/docs` and `/redoc` on the running server
- Human-written reference: [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md)

Example routes (no `/api/v1` prefix on the current app): `/auth/me`,
`/patients`, `/checkins`, `/dashboard`, `/therapists` — see the OpenAPI UI for
the full list and auth requirements.

## Security notes

- **Auth0 JWT** validation for protected operations (see `app/core/auth.py` and
  routers)
- Role-aware profiles (`patient` / `therapist`) synced via auth routes
- Audit middleware logs PHI-touching requests when configured

## Acknowledgments

- Auth0 for authentication
- Snowflake
- SmartSpectra (Presage)
- Optional: Twilio — when integrated via configuration
