package com.example.cadence.api

object PlaceholderData {

    val patients = listOf(
        Patient(
            patientId = 1, name = "Alex Rivera", email = "alex.rivera@email.com",
            assignedScale = "PHQ-9", checkinCount = 4, latestScore = 7,
            latestCheckin = "2026-03-15"
        ),
        Patient(
            patientId = 2, name = "Morgan Chen", email = "morgan.chen@email.com",
            assignedScale = "GAD-7", checkinCount = 3, latestScore = 13,
            latestCheckin = "2026-03-15"
        ),
        Patient(
            patientId = 3, name = "Jordan Taylor", email = "jordan.taylor@email.com",
            assignedScale = "PHQ-9", checkinCount = 3, latestScore = 16,
            latestCheckin = "2026-03-19"
        )
    )

    val similarSessions = listOf(
        SimilarSession(
            sessionId = 101, checkinDate = "2026-01-25", scaleScore = 18,
            hrvValue = 45.2, breathingRate = 18.5, distressRating = 8,
            situationText = "Major deadline conflict with my manager",
            copingText = "Tried deep breathing", scaleType = "PHQ-9", similarity = 0.94
        ),
        SimilarSession(
            sessionId = 102, checkinDate = "2026-02-08", scaleScore = 14,
            hrvValue = 51.2, breathingRate = 17.0, distressRating = 6,
            situationText = "Project deadline passed",
            copingText = "Slept in on weekend", scaleType = "PHQ-9", similarity = 0.87
        ),
        SimilarSession(
            sessionId = 103, checkinDate = "2026-02-22", scaleScore = 10,
            hrvValue = 58.4, breathingRate = 15.8, distressRating = 4,
            situationText = "Handled work issue better",
            copingText = "Used therapy techniques", scaleType = "PHQ-9", similarity = 0.79
        ),
        SimilarSession(
            sessionId = 104, checkinDate = "2026-03-15", scaleScore = 7,
            hrvValue = 63.5, breathingRate = 14.5, distressRating = 2,
            situationText = "Quiet weekend felt peaceful",
            copingText = "Read a book", scaleType = "PHQ-9", similarity = 0.72
        )
    )

    val sessionDetail = SessionDetailResponse(
        session = SessionDetail(
            sessionId = 101, patientId = 1, checkinDate = "2026-01-25",
            scaleType = "PHQ-9", scaleScore = 18, hrvValue = 45.2,
            breathingRate = 18.5, pulseRate = 78.0, distressRating = 8,
            situationText = "Major deadline conflict with my manager",
            copingText = "Tried deep breathing"
        ),
        questions = listOf(
            QuestionVital("Q1", "Little interest or pleasure", 2, 44.1, true, "PHQ-9"),
            QuestionVital("Q2", "Feeling down, depressed, hopeless", 3, 42.5, true, "PHQ-9"),
            QuestionVital("Q3", "Trouble falling or staying asleep", 2, 46.0, true, "PHQ-9"),
            QuestionVital("Q4", "Feeling tired or having little energy", 2, 45.8, false, "PHQ-9"),
            QuestionVital("Q5", "Poor appetite or overeating", 1, null, false, "PHQ-9"),
            QuestionVital("Q6", "Feeling bad about yourself", 3, 41.2, true, "PHQ-9"),
            QuestionVital("Q7", "Trouble concentrating", 2, 44.5, true, "PHQ-9"),
            QuestionVital("Q8", "Moving/speaking slowly or being fidgety", 1, null, false, "PHQ-9"),
            QuestionVital("Q9", "Thoughts of hurting yourself", 2, 40.0, true, "PHQ-9")
        )
    )
}
