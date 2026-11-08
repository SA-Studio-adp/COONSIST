package com.example.data.local

import com.example.data.model.Exercise

object PreloadedExerciseData {
    val items = listOf(
        Exercise(
            name = "Barbell Flat Bench Press",
            targetMuscle = "Chest",
            equipment = "Barbell & Bench",
            description = "Compound pressing movement targeting pectoralis major, anterior deltoids and triceps.",
            metValue = 6.0f
        ),
        Exercise(
            name = "Incline Dumbbell Press",
            targetMuscle = "Chest",
            equipment = "Dumbbells & Incline Bench",
            description = "Emphasizes clavicular head of pectorals with greater range of motion.",
            metValue = 5.5f
        ),
        Exercise(
            name = "Push-ups",
            targetMuscle = "Chest",
            equipment = "Bodyweight",
            description = "Fundamental compound bodyweight movement for upper body pressing and core stability.",
            metValue = 4.2f
        ),
        Exercise(
            name = "Barbell Back Squat",
            targetMuscle = "Legs",
            equipment = "Barbell & Squat Rack",
            description = "King of lower body compound lifts targeting quadriceps, glutes, adductors and spine erectors.",
            metValue = 7.0f
        ),
        Exercise(
            name = "Romanian Deadlift (RDL)",
            targetMuscle = "Legs",
            equipment = "Barbell or Dumbbells",
            description = "Hinge movement focusing on posterior chain: hamstrings, gluteus maximus and lower back.",
            metValue = 6.0f
        ),
        Exercise(
            name = "Leg Press",
            targetMuscle = "Legs",
            equipment = "Machine",
            description = "Safe heavy leg volume targeting quads without heavy spinal loading.",
            metValue = 5.0f
        ),
        Exercise(
            name = "Pull-Ups / Chin-Ups",
            targetMuscle = "Back",
            equipment = "Pull-up Bar",
            description = "Vertical pulling powerhouse building latissimus dorsi, rhomboids, and biceps.",
            metValue = 6.5f
        ),
        Exercise(
            name = "Barbell Bent-Over Row",
            targetMuscle = "Back",
            equipment = "Barbell",
            description = "Horizontal pull building upper back thickness, lats and mid-traps.",
            metValue = 5.5f
        ),
        Exercise(
            name = "Standing Overhead Press (OHP)",
            targetMuscle = "Shoulders",
            equipment = "Barbell",
            description = "Vertical compound press building deltoid strength and core rigidity.",
            metValue = 5.5f
        ),
        Exercise(
            name = "Dumbbell Lateral Raise",
            targetMuscle = "Shoulders",
            equipment = "Dumbbells",
            description = "Isolation exercise for lateral deltoid head to build shoulder width.",
            metValue = 4.0f
        ),
        Exercise(
            name = "Barbell Bicep Curl",
            targetMuscle = "Arms",
            equipment = "Barbell / EZ Bar",
            description = "Classic bicep flexion movement.",
            metValue = 4.0f
        ),
        Exercise(
            name = "Tricep Overhead Cable Extension",
            targetMuscle = "Arms",
            equipment = "Cable Machine",
            description = "Stretches and isolates the long head of the triceps.",
            metValue = 4.0f
        ),
        Exercise(
            name = "Plank & Core Hollow Hold",
            targetMuscle = "Core",
            equipment = "Mat",
            description = "Isometric core endurance strengthening transverse abdominis.",
            metValue = 3.5f
        ),
        Exercise(
            name = "HIIT Sprint Intervals / Treadmill",
            targetMuscle = "Cardio",
            equipment = "Treadmill or Track",
            description = "High intensity interval sprint training for cardiovascular stamina and fat oxidation.",
            metValue = 9.5f
        ),
        Exercise(
            name = "Jump Rope (Cardio Burn)",
            targetMuscle = "Cardio",
            equipment = "Jump Rope",
            description = "Rhythmic high calorie burning cardio and calf coordination.",
            metValue = 10.0f
        ),
        Exercise(
            name = "Incline Brisk Walking",
            targetMuscle = "Cardio",
            equipment = "Treadmill",
            description = "Low impact steady state cardio preserving muscle mass.",
            metValue = 4.5f
        )
    )
}
