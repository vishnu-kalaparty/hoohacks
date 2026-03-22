-- Auris Mock Data for Testing
-- Run this after 01_schema.sql

-- =====================================================
-- THERAPISTS
-- =====================================================
INSERT INTO CADENCE.PUBLIC.THERAPISTS (AUTH0_USER_ID, NAME, EMAIL) VALUES
('auth0|therapist_001', 'Dr. Sarah Johnson', 'sarah.johnson@auris.demo'),
('auth0|therapist_002', 'Dr. Michael Chen', 'michael.chen@auris.demo');

-- =====================================================
-- PATIENT 1: PHQ-9, Improving trend
-- =====================================================
INSERT INTO CADENCE.PUBLIC.PATIENTS (AUTH0_USER_ID, NAME, EMAIL, THERAPIST_ID, ASSIGNED_SCALE, APPOINTMENT_DAY, TIMEZONE) 
SELECT 
    'auth0|patient_001', 
    'Alex Rivera', 
    'alex.rivera@email.com', 
    THERAPIST_ID,
    'PHQ-9', 
    'Monday', 
    'America/New_York'
FROM CADENCE.PUBLIC.THERAPISTS 
WHERE AUTH0_USER_ID = 'auth0|therapist_001';

-- Check-in history for Patient 1 (improving over time)
INSERT INTO CADENCE.PUBLIC.CHECKIN_SESSIONS 
    (PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE, HRV_VALUE, BREATHING_RATE, PULSE_RATE, 
     DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT)
SELECT 
    p.PATIENT_ID,
    DATEADD(day, -56, CURRENT_DATE),
    'PHQ-9', 18, 45.2, 18.5, 78, 8,
    'Major deadline conflict with my manager',
    'Tried deep breathing'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_001'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -42, CURRENT_DATE), 'PHQ-9', 14, 51.2, 17.0, 71, 6,
    'Project deadline passed', 'Slept in on weekend'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_001'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -28, CURRENT_DATE), 'PHQ-9', 10, 58.4, 15.8, 66, 4,
    'Handled work issue better', 'Used therapy techniques'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_001'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -7, CURRENT_DATE), 'PHQ-9', 7, 63.5, 14.5, 60, 2,
    'Quiet weekend felt peaceful', 'Read a book'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_001';

-- =====================================================
-- PATIENT 2: GAD-7, Stable
-- =====================================================
INSERT INTO CADENCE.PUBLIC.PATIENTS (AUTH0_USER_ID, NAME, EMAIL, THERAPIST_ID, ASSIGNED_SCALE, APPOINTMENT_DAY, TIMEZONE) 
SELECT 
    'auth0|patient_002', 'Morgan Chen', 'morgan.chen@email.com', 
    THERAPIST_ID, 'GAD-7', 'Wednesday', 'America/Los_Angeles'
FROM CADENCE.PUBLIC.THERAPISTS WHERE AUTH0_USER_ID = 'auth0|therapist_001';

INSERT INTO CADENCE.PUBLIC.CHECKIN_SESSIONS 
    (PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE, HRV_VALUE, BREATHING_RATE, PULSE_RATE, 
     DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT)
SELECT 
    p.PATIENT_ID, DATEADD(day, -35, CURRENT_DATE), 'GAD-7', 14, 52.0, 16.5, 72, 7,
    'Social gathering anxiety', 'Left early'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_002'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -21, CURRENT_DATE), 'GAD-7', 14, 50.8, 17.0, 74, 7,
    'Another social event', 'Used breathing'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_002'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -7, CURRENT_DATE), 'GAD-7', 13, 52.5, 16.5, 71, 6,
    'Dinner with colleagues', 'Focused on one'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_002';

-- =====================================================
-- PATIENT 3: PHQ-9, Q9 = 2 (FLAG DEMO)
-- =====================================================
INSERT INTO CADENCE.PUBLIC.PATIENTS (AUTH0_USER_ID, NAME, EMAIL, THERAPIST_ID, ASSIGNED_SCALE, APPOINTMENT_DAY, TIMEZONE) 
SELECT 
    'auth0|patient_003', 'Jordan Taylor', 'jordan.taylor@email.com', 
    THERAPIST_ID, 'PHQ-9', 'Friday', 'America/Chicago'
FROM CADENCE.PUBLIC.THERAPISTS WHERE AUTH0_USER_ID = 'auth0|therapist_001';

INSERT INTO CADENCE.PUBLIC.CHECKIN_SESSIONS 
    (PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE, HRV_VALUE, BREATHING_RATE, PULSE_RATE, 
     DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT)
SELECT 
    p.PATIENT_ID, DATEADD(day, -28, CURRENT_DATE), 'PHQ-9', 12, 54.0, 16.0, 70, 6,
    'Felt down but manageable', 'Went for run'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_003'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -21, CURRENT_DATE), 'PHQ-9', 14, 50.0, 17.0, 74, 7,
    'Relationship conflict', 'Journaled'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_003'
UNION ALL
SELECT 
    p.PATIENT_ID, DATEADD(day, -3, CURRENT_DATE), 'PHQ-9', 16, 46.0, 18.5, 79, 8,
    'Feeling hopeless', 'Crisis line'
FROM CADENCE.PUBLIC.PATIENTS p WHERE p.AUTH0_USER_ID = 'auth0|patient_003';

-- Question vitals with Q9 = 2 (elevated)
INSERT INTO CADENCE.PUBLIC.QUESTION_VITALS 
    (SESSION_ID, PATIENT_ID, QUESTION_ID, QUESTION_TEXT, SCALE_TYPE, RESPONSE_VALUE,
     HRV_AT_QUESTION, IS_VITALS_CORRELATED)
SELECT 
    cs.SESSION_ID, cs.PATIENT_ID, 'Q9', 'Thoughts of hurting yourself', 'PHQ-9',
    2, -- ELEVATED
    cs.HRV_VALUE - 3, TRUE
FROM CADENCE.PUBLIC.CHECKIN_SESSIONS cs
JOIN CADENCE.PUBLIC.PATIENTS p ON cs.PATIENT_ID = p.PATIENT_ID
WHERE p.AUTH0_USER_ID = 'auth0|patient_003'
  AND cs.CHECKIN_DATE >= DATEADD(day, -7, CURRENT_DATE);

-- =====================================================
-- SCHEDULES
-- =====================================================
INSERT INTO CADENCE.PUBLIC.CHECKIN_SCHEDULE (PATIENT_ID, SCHEDULED_DATE, COMPLETED)
SELECT p.PATIENT_ID, DATEADD(day, 1, CURRENT_DATE), FALSE
FROM CADENCE.PUBLIC.PATIENTS p;

INSERT INTO CADENCE.PUBLIC.CHECKIN_SCHEDULE (PATIENT_ID, SCHEDULED_DATE, COMPLETED)
SELECT p.PATIENT_ID, DATEADD(day, 3, CURRENT_DATE), FALSE
FROM CADENCE.PUBLIC.PATIENTS p;

-- =====================================================
-- VERIFICATION
-- =====================================================
SELECT 'THERAPISTS' as TABLE_NAME, COUNT(*) as COUNT FROM CADENCE.PUBLIC.THERAPISTS
UNION ALL SELECT 'PATIENTS', COUNT(*) FROM CADENCE.PUBLIC.PATIENTS
UNION ALL SELECT 'CHECKINS', COUNT(*) FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
UNION ALL SELECT 'Q9 FLAGS', COUNT(*) 
FROM CADENCE.PUBLIC.QUESTION_VITALS WHERE QUESTION_ID = 'Q9' AND RESPONSE_VALUE > 1;
