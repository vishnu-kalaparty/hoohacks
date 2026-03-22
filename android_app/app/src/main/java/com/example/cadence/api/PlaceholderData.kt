package com.example.cadence.api

object PlaceholderData {

    val similarSessions = listOf(
        SimilarSession(
            sessionId = 101,
            checkinDate = "2025-01-15",
            scaleScore = 18,
            hrvValue = 42.0,
            breathingRate = 18.5,
            distressRating = 7,
            situationText = "Felt overwhelmed at work, argument with supervisor",
            copingText = "Took a walk, called a friend",
            scaleType = "PHQ-9",
            similarity = 0.94
        ),
        SimilarSession(
            sessionId = 102,
            checkinDate = "2025-01-08",
            scaleScore = 15,
            hrvValue = 48.0,
            breathingRate = 16.2,
            distressRating = 6,
            situationText = "Stress about upcoming deadline, difficulty sleeping",
            copingText = "Journaling, breathing exercises",
            scaleType = "PHQ-9",
            similarity = 0.89
        ),
        SimilarSession(
            sessionId = 103,
            checkinDate = "2024-12-22",
            scaleScore = 21,
            hrvValue = 38.0,
            breathingRate = 20.1,
            distressRating = 8,
            situationText = "Holiday stress and family conflict",
            copingText = "Meditation, therapist session",
            scaleType = "PHQ-9",
            similarity = 0.85
        ),
        SimilarSession(
            sessionId = 104,
            checkinDate = "2024-12-10",
            scaleScore = 12,
            hrvValue = 55.0,
            breathingRate = 15.0,
            distressRating = 5,
            situationText = "Feeling isolated, not attending social events",
            copingText = "Reached out to old friends",
            scaleType = "PHQ-9",
            similarity = 0.82
        ),
        SimilarSession(
            sessionId = 105,
            checkinDate = "2024-11-28",
            scaleScore = 16,
            hrvValue = 44.0,
            breathingRate = 17.8,
            distressRating = 7,
            situationText = "Performance review anxiety, self-doubt",
            copingText = "Talked to mentor, CBT exercises",
            scaleType = "PHQ-9",
            similarity = 0.78
        ),
        SimilarSession(
            sessionId = 106,
            checkinDate = "2024-11-15",
            scaleScore = 10,
            hrvValue = 58.0,
            breathingRate = 14.5,
            distressRating = 4,
            situationText = "Good week overall, minor frustration at work",
            copingText = "Exercise, good sleep",
            scaleType = "PHQ-9",
            similarity = 0.74
        ),
        SimilarSession(
            sessionId = 107,
            checkinDate = "2024-11-01",
            scaleScore = 19,
            hrvValue = 40.0,
            breathingRate = 19.0,
            distressRating = 8,
            situationText = "Relationship difficulties, feeling unsupported",
            copingText = "Couples counseling scheduled",
            scaleType = "PHQ-9",
            similarity = 0.71
        )
    )

    val sessionDetail = SessionDetailResponse(
        session = SessionInfo(
            sessionId = 101,
            patientId = 1,
            checkinDate = "2025-01-15",
            scaleType = "PHQ-9",
            scaleScore = 18,
            hrvValue = 42.0,
            breathingRate = 18.5,
            pulseRate = 88.0,
            distressRating = 7,
            situationText = "Felt overwhelmed at work, had an argument with supervisor about project priorities. Couldn't focus for the rest of the day.",
            copingText = "Took a 20-minute walk during lunch. Called a friend in the evening. Did 10 minutes of guided breathing before bed."
        ),
        questions = listOf(
            QuestionVital("Q1", "Little interest or pleasure in doing things", 2, 45.0, true, "PHQ-9"),
            QuestionVital("Q2", "Feeling down, depressed, or hopeless", 3, 38.0, true, "PHQ-9"),
            QuestionVital("Q3", "Trouble falling or staying asleep", 2, 42.0, true, "PHQ-9"),
            QuestionVital("Q4", "Feeling tired or having little energy", 3, 40.0, true, "PHQ-9"),
            QuestionVital("Q5", "Poor appetite or overeating", 1, 50.0, false, "PHQ-9"),
            QuestionVital("Q6", "Feeling bad about yourself", 2, 36.0, true, "PHQ-9"),
            QuestionVital("Q7", "Trouble concentrating on things", 2, 41.0, true, "PHQ-9"),
            QuestionVital("Q8", "Moving or speaking slowly", 1, 48.0, false, "PHQ-9"),
            QuestionVital("Q9", "Thoughts that you would be better off dead", 2, 32.0, true, "PHQ-9")
        )
    )
}
