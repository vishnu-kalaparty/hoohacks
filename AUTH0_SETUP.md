# Auth0 Setup Guide

This guide will help you set up Auth0 authentication for the Auris hackathon project.

## Prerequisites

1. An Auth0 account (free tier available at https://auth0.com)
2. Python dependencies installed: `pip install -r backend/requirements.txt`

## Step 1: Create an Auth0 Application

1. Log in to your Auth0 Dashboard (https://manage.auth0.com)
2. Go to **Applications > Create Application**
3. Choose **"Single Page Web Applications"** for React/frontend
   - Or **"Native"** for the Android app
4. Note down the **Domain** and **Client ID**

## Step 2: Create an Auth0 API

1. Go to **Applications > APIs > Create API**
2. Set a name (e.g., "Auris API")
3. Set an **Identifier** (e.g., `https://auris-api`) - this becomes your `AUTH0_API_AUDIENCE`
4. Choose **RS256** signing algorithm
5. Save the API

## Step 3: Configure Environment Variables

Update your `.env` file with the Auth0 credentials:

```env
# Auth0 Configuration
AUTH0_DOMAIN=your-tenant.auth0.com
AUTH0_API_AUDIENCE=https://auris-api
AUTH0_CLIENT_ID=your-application-client-id
AUTH0_CLIENT_SECRET=your-application-client-secret (for backend only)
AUTH0_ALGORITHMS=RS256
```

## Step 4: Test the Backend

1. Start the backend server:
   ```bash
   cd backend
   uvicorn main:app --reload
   ```

2. Test the public endpoints (no auth required):
   ```bash
   curl http://localhost:8000/
   curl http://localhost:8000/auth/config
   ```

3. Test a protected endpoint (requires auth token):
   ```bash
   # This should fail with 403
   curl http://localhost:8000/me
   
   # Get a token from Auth0 and test:
   curl -H "Authorization: Bearer YOUR_AUTH0_TOKEN" http://localhost:8000/me
   ```

## Step 5: Configure Android App (Optional)

Add Auth0 to your Android app by adding the Auth0 Android SDK dependency:

```kotlin
// In app/build.gradle.kts
dependencies {
    implementation("com.auth0.android:auth0:2.10.2")
}
```

Update `AndroidManifest.xml`:

```xml
<activity
    android:name="com.auth0.android.provider.WebAuthActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https"
              android:host="YOUR_AUTH0_DOMAIN"
              android:pathPrefix="/android/com.example.cadence/callback" />
    </intent-filter>
</activity>
```

## API Endpoints

### Public Endpoints (No Authentication)
- `GET /` - Health check
- `GET /health` - Database health check
- `GET /auth/config` - Get Auth0 configuration

### Protected Endpoints (Requires Auth0 JWT)
- `GET /me` - Get current user info
- `GET /protected` - Test protected route
- `GET /patients/{patient_id}` - Get patient info
- `GET /patients/{patient_id}/history` - Get patient history
- `POST /checkins/` - Submit check-in
- `GET /dashboard/therapists/{therapist_id}/patients` - Get therapist's patients

## Getting an Auth0 Token for Testing

### Option 1: Using Auth0 Test Token

1. In Auth0 Dashboard, go to your API > **Test**
2. Select your application
3. Copy the access token provided

### Option 2: Using curl

```bash
# Replace with your actual credentials
curl --request POST \
  --url https://YOUR_DOMAIN/oauth/token \
  --header 'content-type: application/json' \
  --data '{
    "client_id": "YOUR_CLIENT_ID",
    "client_secret": "YOUR_CLIENT_SECRET",
    "audience": "YOUR_API_AUDIENCE",
    "grant_type": "client_credentials"
  }'
```

## Troubleshooting

### "Unable to find appropriate key" Error
- Make sure your Auth0 domain is correct
- Verify the token is not expired
- Check that the token was issued for the correct audience

### "Invalid claims" Error
- Verify `AUTH0_API_AUDIENCE` matches your API identifier in Auth0
- Ensure the issuer URL is correct (`https://your-domain.auth0.com/`)

### CORS Errors
- Update `ALLOWED_HOSTS` in your `.env` file with your frontend URL
- Default allows all origins (`["*"]`) in development

## Security Notes

- Never commit `.env` files with real credentials
- The `AUTH0_CLIENT_SECRET` should only be used server-side
- For production, configure proper CORS origins
- Enable HTTPS in production
