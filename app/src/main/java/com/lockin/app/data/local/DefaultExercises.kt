package com.lockin.app.data.local

import com.lockin.app.data.local.models.ExerciseDetailEntity

/**
 * Default exercise library – seeded once on first launch.
 * Covers major compounds and isolations for common muscle groups.
 */
object DefaultExercises {

    fun all(): List<ExerciseDetailEntity> = listOf(
        // ── Chest ────────────────────────────────────────
        ex("bench_press", "Barbell Bench Press", "CHEST", listOf("TRICEPS", "SHOULDERS"), listOf("BARBELL", "BENCH"), "COMPOUND", "MODERATE"),
        ex("incline_bench", "Incline Dumbbell Press", "CHEST", listOf("SHOULDERS", "TRICEPS"), listOf("DUMBBELL", "BENCH"), "COMPOUND", "MODERATE"),
        ex("cable_fly", "Cable Fly", "CHEST", emptyList(), listOf("CABLE"), "ISOLATION", "EASY"),
        ex("push_up", "Push-Up", "CHEST", listOf("TRICEPS", "SHOULDERS"), listOf("BODYWEIGHT"), "COMPOUND", "EASY"),

        // ── Back ─────────────────────────────────────────
        ex("barbell_row", "Barbell Row", "BACK", listOf("BICEPS", "REAR_DELTS"), listOf("BARBELL"), "COMPOUND", "MODERATE"),
        ex("pull_up", "Pull-Up", "BACK", listOf("BICEPS"), listOf("BODYWEIGHT", "PULL_UP_BAR"), "COMPOUND", "HARD"),
        ex("lat_pulldown", "Lat Pulldown", "BACK", listOf("BICEPS"), listOf("CABLE"), "COMPOUND", "EASY"),
        ex("seated_row", "Seated Cable Row", "BACK", listOf("BICEPS", "REAR_DELTS"), listOf("CABLE"), "COMPOUND", "MODERATE"),

        // ── Shoulders ────────────────────────────────────
        ex("ohp", "Overhead Press", "SHOULDERS", listOf("TRICEPS"), listOf("BARBELL"), "COMPOUND", "MODERATE"),
        ex("lateral_raise", "Lateral Raise", "SHOULDERS", emptyList(), listOf("DUMBBELL"), "ISOLATION", "EASY"),
        ex("face_pull", "Face Pull", "SHOULDERS", listOf("REAR_DELTS"), listOf("CABLE"), "ISOLATION", "EASY"),

        // ── Legs ─────────────────────────────────────────
        ex("squat", "Barbell Squat", "QUADS", listOf("GLUTES", "HAMSTRINGS"), listOf("BARBELL", "SQUAT_RACK"), "COMPOUND", "HARD"),
        ex("leg_press", "Leg Press", "QUADS", listOf("GLUTES"), listOf("MACHINE"), "COMPOUND", "MODERATE"),
        ex("romanian_dl", "Romanian Deadlift", "HAMSTRINGS", listOf("GLUTES", "BACK"), listOf("BARBELL"), "COMPOUND", "MODERATE"),
        ex("leg_curl", "Leg Curl", "HAMSTRINGS", emptyList(), listOf("MACHINE"), "ISOLATION", "EASY"),
        ex("leg_extension", "Leg Extension", "QUADS", emptyList(), listOf("MACHINE"), "ISOLATION", "EASY"),
        ex("calf_raise", "Standing Calf Raise", "CALVES", emptyList(), listOf("MACHINE"), "ISOLATION", "EASY"),

        // ── Arms ─────────────────────────────────────────
        ex("barbell_curl", "Barbell Curl", "BICEPS", emptyList(), listOf("BARBELL"), "ISOLATION", "EASY"),
        ex("hammer_curl", "Hammer Curl", "BICEPS", listOf("FOREARMS"), listOf("DUMBBELL"), "ISOLATION", "EASY"),
        ex("tricep_pushdown", "Tricep Pushdown", "TRICEPS", emptyList(), listOf("CABLE"), "ISOLATION", "EASY"),
        ex("skull_crusher", "Skull Crusher", "TRICEPS", emptyList(), listOf("BARBELL", "BENCH"), "ISOLATION", "MODERATE"),

        // ── Core / Full Body ─────────────────────────────
        ex("deadlift", "Deadlift", "BACK", listOf("HAMSTRINGS", "GLUTES", "CORE"), listOf("BARBELL"), "COMPOUND", "HARD"),
        ex("plank", "Plank", "CORE", emptyList(), listOf("BODYWEIGHT"), "ISOMETRIC", "EASY"),
        ex("hanging_leg_raise", "Hanging Leg Raise", "CORE", emptyList(), listOf("PULL_UP_BAR"), "COMPOUND", "MODERATE")
    )

    private fun ex(
        id: String,
        name: String,
        primary: String,
        secondary: List<String>,
        equipment: List<String>,
        pattern: String,
        difficulty: String
    ) = ExerciseDetailEntity(
        id = id,
        name = name,
        primaryMuscleGroup = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        movementPattern = pattern,
        difficulty = difficulty
    )
}

