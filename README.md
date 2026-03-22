# Auris: Between Session Intelligence

A full-stack web application for PTSD patient monitoring between therapy sessions.

## Overview

**Auris** helps therapists monitor patient well-being between sessions through:
- Regular check-ins with clinical scales (PHQ-9, GAD-7)
- Continuous vitals monitoring during check-ins
- AI-powered situation clustering and trend analysis
- Secure, HIPAA-compliant data storage in Snowflake

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AURIS PLATFORM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │   React      │    │   FastAPI    │    │  Streamlit   │                   │
│  │   Frontend   │◄──►│   Backend    │◄──►│  Dashboard   │                   │
│  │  (Patient)   │    │   (Auth0)    │    │ (Therapist)  │                   │
│  └──────────────┘    └──────┬───────┘    └──────────────┘                   │
│                             │                                                │
│                             ▼                                                │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │ SmartSpectra │    │   Snowflake  │    │    Twilio    │                   │
│  │    SDK       │    │  (Cortex AI) │    │    SMS       │                   │
│  │  (Vitals)    │    │  (Database)  │    │ (Reminders)  │                   │
│  └──────────────┘    └──────────────┘    └──────────────┘                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | React + Auth0 React SDK |
| Backend | FastAPI (Python) |
| Database | Snowflake + Cortex AI |
| Vitals | SmartSpectra SDK (Presage Technologies) |
| Notifications | Twilio SMS |
| Dashboard | Streamlit |
| Auth | Auth0 |

## Project Structure

```
auris/
├── backend/                 # FastAPI Backend
│   ├── app/
│   │   ├── api/v1/         # API endpoints
│   │   ├── core/           # Auth, config, database
│   │   ├── schemas/        # Pydantic models
│   │   └── services/       # Business logic
│   ├── tests/              # Test suite
│   └── requirements.txt
├── frontend/               # React Frontend
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── pages/          # Page components
│   │   └── contexts/       # Auth context
│   └── package.json
├── dashboard/              # Streamlit Therapist Dashboard
│   └── app.py
├── snowflake/              # Snowflake SQL Scripts
│   ├── 01_schema.sql       # Database schema
│   ├── 02_embedding_pipeline.sql
│   ├── 03_trend_queries.sql
│   └── 04_demo_data.sql
└── README.md
```

## Prerequisites

- Python 3.9+
- Node.js 18+
- Snowflake account
- Auth0 account
- Twilio account (for SMS)
- SmartSpectra SDK access (Presage Technologies)

## Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd auris
```

### 2. Auth0 Configuration

#### Create Auth0 Application

1. Go to [Auth0 Dashboard](https://manage.auth0.com/)
2. Create a new "Single Page Application" for React frontend
3. Create a new "Machine to Machine Application" for Streamlit
4. Create an API with identifier (e.g., `https://auris-api`)

#### Configure Auth0 Roles

1. Go to User Management > Roles
2. Create two roles: `patient` and `therapist`
3. Assign roles to users as needed

#### Auth0 Environment Variables

Backend `.env`:
```
AUTH0_DOMAIN=your-domain.auth0.com
AUTH0_API_AUDIENCE=https://auris-api
AUTH0_CLIENT_ID=your-client-id
AUTH0_CLIENT_SECRET=your-client-secret
```

Frontend `.env`:
```
REACT_APP_AUTH0_DOMAIN=your-domain.auth0.com
REACT_APP_AUTH0_CLIENT_ID=your-spa-client-id
REACT_APP_AUTH0_AUDIENCE=https://auris-api
```

### 3. Snowflake Setup

1. Run the SQL scripts in `snowflake/` folder in order:
   ```bash
   # In Snowflake worksheet
   @01_schema.sql
   @02_embedding_pipeline.sql
   @03_trend_queries.sql
   @04_demo_data.sql
   ```

2. Configure Snowflake credentials in `backend/.env`:
   ```
   SNOWFLAKE_USER=your-user
   SNOWFLAKE_PASSWORD=your-password
   SNOWFLAKE_ACCOUNT=your-account
   SNOWFLAKE_DATABASE=AURIS_DB
   ```

### 4. Backend Setup

```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# Edit .env with your credentials
uvicorn app.main:app --reload
```

Backend runs at: http://localhost:8000

### 5. Frontend Setup

```bash
cd frontend
npm install
cp .env.example .env
# Edit .env with your Auth0 credentials
npm start
```

Frontend runs at: http://localhost:3000

### 6. Streamlit Dashboard Setup

```bash
cd dashboard
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# Edit .env with your credentials
streamlit run app.py
```

Dashboard runs at: http://localhost:8501

## API Documentation

Once the backend is running, view API docs at:
- Swagger UI: http://localhost:8000/api/v1/docs
- ReDoc: http://localhost:8000/api/v1/redoc

### Key Endpoints

| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/auth/me` | GET | Get current user | JWT |
| `/api/v1/patients/me` | GET | Get patient info | Patient |
| `/api/v1/checkins/` | POST | Submit check-in | Patient |
| `/api/v1/dashboard/patients/compliance` | GET | Get patient list | Therapist |
| `/api/v1/dashboard/patients/{id}/brief` | GET | Get patient brief | Therapist |

## User Flows

### Patient Flow

1. Login via Auth0 → Redirected to Patient Dashboard
2. View next scheduled check-in date
3. On check-in day: Click "Start Check-in"
4. **Step 1**: Camera activates for vitals monitoring
5. **Step 2**: Answer assigned scale (PHQ-9 or GAD-7)
   - Vitals captured at vitals-correlated questions
   - 5-second pause for snapshot capture
6. **Step 3**: Answer 3 additional questions
7. **Step 4**: Submit check-in
8. Data processed: embedding + clustering pipeline

### Therapist Flow

1. Login via Auth0 → Redirected to Streamlit Dashboard
2. View patient list with compliance indicators
3. Click patient to view Session Brief:
   - **Section 1**: Weekly overview charts
   - **Section 2**: Question-level vitals sparklines
   - **Section 3**: Situation clusters
   - **Section 4**: Comparison panel with confirmation
   - **Section 5**: Clinical flags (PHQ-9 Q9)
   - **Section 6**: AI discussion starter

## Snowflake Schema

### Core Tables

- `patients` - Patient profiles linked to Auth0
- `therapists` - Therapist profiles linked to Auth0
- `checkin_sessions` - Check-in records with embeddings
- `question_vitals` - Question-level vitals snapshots
- `checkin_schedule` - Scheduled check-ins
- `scale_questions` - PHQ-9 and GAD-7 reference data

### Embedding Pipeline

1. `SNOWFLAKE.CORTEX.EMBED_TEXT_768('e5-base-v2', situation_text)`
2. Cosine similarity matching (>0.82 threshold)
3. Cluster assignment or creation
4. LAG() window functions for trend computation

## Security

- **Auth0 JWT** validation on all API endpoints
- **Role-based access**: Patient vs Therapist
- **PHI encrypted** at rest in Snowflake
- **No video/audio stored** - only derived vitals values
- **No automated crisis alerts** - flags for therapist review only

## Demo Data

The system includes 3 demo patients:

1. **Alex Rivera** (PHQ-9) - Improving over 8 weeks, work conflict cluster
2. **Morgan Chen** (GAD-7) - Stable scores, social anxiety cluster
3. **Jordan Taylor** (PHQ-9) - Q9 elevated (demonstrates flag feature)

## Development

### Running Tests

```bash
cd backend
pytest
```

### Adding New Features

1. Backend: Add endpoints in `app/api/v1/endpoints/`
2. Frontend: Add pages in `frontend/src/pages/`
3. Dashboard: Update `dashboard/app.py`
4. Database: Add migrations in `snowflake/`

## Deployment

### Backend (Docker)

```bash
cd backend
docker build -t auris-backend .
docker run -p 8000:8000 --env-file .env auris-backend
```

### Frontend (Static Hosting)

```bash
cd frontend
npm run build
# Deploy build/ folder to Netlify/Vercel
```

### Dashboard (Streamlit Cloud)

```bash
# Push to GitHub, connect to Streamlit Cloud
# Set secrets in Streamlit Cloud dashboard
```

## License

MIT License - See LICENSE file

## Support

For support, contact: support@auris-health.com

## Acknowledgments

- SmartSpectra SDK by Presage Technologies
- Snowflake Cortex AI
- Auth0 for authentication
- Twilio for notifications
