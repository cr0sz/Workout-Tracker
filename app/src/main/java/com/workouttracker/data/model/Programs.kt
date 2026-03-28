package com.workouttracker.data.model

// ── Data models ───────────────────────────────────────────────────────────────

data class ProgramExercise(
    val name: String,
    val sets: Int,
    val reps: String,        // "8-12", "5", "AMRAP", "60 sec"
    val notes: String = ""
)

data class ProgramDay(
    val dayNumber: Int,
    val name: String,        // "Push Day", "Squat Focus", etc.
    val focus: String,       // short tag e.g. "Chest & Triceps"
    val exercises: List<ProgramExercise>,
    val isRestDay: Boolean = false,
    val cardioNotes: String = "" // for cardio-only days
)

data class ProgramTemplate(
    val id: String,
    val name: String,
    val category: ProgramCategory,
    val tagline: String,
    val description: String,
    val daysPerWeek: Int,
    val durationWeeks: Int,
    val difficulty: Difficulty,
    val days: List<ProgramDay>   // one week (repeating)
)

enum class ProgramCategory(val label: String, val emoji: String) {
    BODYBUILDING("Bodybuilding", "💪"),
    POWERLIFTING("Powerlifting", "🏋️"),
    HYROX("Hyrox", "⚡"),
    RUNNING("Running", "🏃"),
    STRONGMAN("Strongman", "🪨"),
    CALISTHENICS("Calisthenics", "🤸")
}

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

// ── Programs ──────────────────────────────────────────────────────────────────

val ALL_PROGRAMS: List<ProgramTemplate> = listOf(

    // ── BODYBUILDING ─────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "bro_ppl",
        name          = "Push / Pull / Legs",
        category      = ProgramCategory.BODYBUILDING,
        tagline       = "Classic 6-day hypertrophy split",
        description   = "The most popular bodybuilding split. Two full cycles per week hitting each muscle group twice for maximum hypertrophy. Best for intermediate lifters who want size and definition.",
        daysPerWeek   = 6,
        durationWeeks = 12,
        difficulty    = Difficulty.INTERMEDIATE,
        days          = listOf(
            ProgramDay(1, "Push A", "Chest & Shoulders", listOf(
                ProgramExercise("Bench Press",            4, "6-8",  "Heavy compound"),
                ProgramExercise("Overhead Press",         3, "8-10"),
                ProgramExercise("Incline Bench Press",    3, "10-12"),
                ProgramExercise("Lateral Raise",          4, "15-20","Slow & controlled"),
                ProgramExercise("Tricep Pushdown",        3, "12-15"),
                ProgramExercise("Skull Crusher",          3, "10-12")
            )),
            ProgramDay(2, "Pull A", "Back & Biceps", listOf(
                ProgramExercise("Deadlift",               4, "4-6",  "Primary pull"),
                ProgramExercise("Barbell Row",            3, "8-10"),
                ProgramExercise("Lat Pulldown",           3, "10-12"),
                ProgramExercise("Cable Row",              3, "12-15"),
                ProgramExercise("Face Pull",              3, "15-20","Rear delts"),
                ProgramExercise("Bicep Curl",             3, "10-12"),
                ProgramExercise("Hammer Curl",            2, "12-15")
            )),
            ProgramDay(3, "Legs A", "Quads & Glutes", listOf(
                ProgramExercise("Squat",                  4, "6-8",  "Primary leg movement"),
                ProgramExercise("Romanian Deadlift",      3, "10-12"),
                ProgramExercise("Leg Press",              3, "12-15"),
                ProgramExercise("Leg Extension",          3, "15-20"),
                ProgramExercise("Leg Curl",               3, "12-15"),
                ProgramExercise("Calf Raise",             4, "15-20")
            )),
            ProgramDay(4, "Push B", "Shoulders & Chest", listOf(
                ProgramExercise("Overhead Press",         4, "6-8",  "Heavy compound"),
                ProgramExercise("Incline Bench Press",    3, "8-10"),
                ProgramExercise("Dumbbell Fly",           3, "12-15","Chest isolation"),
                ProgramExercise("Lateral Raise",          4, "20-25","High rep burnout"),
                ProgramExercise("Front Raise",            2, "12-15"),
                ProgramExercise("Tricep Extension",       4, "12-15")
            )),
            ProgramDay(5, "Pull B", "Back Width & Biceps", listOf(
                ProgramExercise("Pull-up",                4, "AMRAP","Bodyweight"),
                ProgramExercise("Dumbbell Row",           3, "10-12","Each arm"),
                ProgramExercise("Lat Pulldown",           3, "12-15","Close grip"),
                ProgramExercise("Shrug",                  3, "12-15"),
                ProgramExercise("Face Pull",              3, "20-25"),
                ProgramExercise("Preacher Curl",          3, "10-12"),
                ProgramExercise("Concentration Curl",     2, "12-15")
            )),
            ProgramDay(6, "Legs B", "Hamstrings & Glutes", listOf(
                ProgramExercise("Romanian Deadlift",      4, "8-10", "Hip hinge focus"),
                ProgramExercise("Bulgarian Split Squat",  3, "10-12","Each leg"),
                ProgramExercise("Leg Press",              3, "15-20","Feet high & wide"),
                ProgramExercise("Leg Curl",               4, "12-15"),
                ProgramExercise("Hip Thrust",             3, "12-15"),
                ProgramExercise("Calf Raise",             4, "20-25")
            )),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    ProgramTemplate(
        id            = "bro_upper_lower",
        name          = "Upper / Lower",
        category      = ProgramCategory.BODYBUILDING,
        tagline       = "4-day strength & size program",
        description   = "A proven 4-day split alternating upper and lower body sessions. Great for beginners moving to intermediate who want balanced muscle development and strength gains.",
        daysPerWeek   = 4,
        durationWeeks = 10,
        difficulty    = Difficulty.BEGINNER,
        days          = listOf(
            ProgramDay(1, "Upper A", "Strength Focus", listOf(
                ProgramExercise("Bench Press",            4, "5",    "Heavy, same weight"),
                ProgramExercise("Barbell Row",            4, "5"),
                ProgramExercise("Overhead Press",         3, "8"),
                ProgramExercise("Lat Pulldown",           3, "10"),
                ProgramExercise("Bicep Curl",             3, "10-12"),
                ProgramExercise("Tricep Pushdown",        3, "10-12")
            )),
            ProgramDay(2, "Lower A", "Strength Focus", listOf(
                ProgramExercise("Squat",                  4, "5",    "Heavy compound"),
                ProgramExercise("Romanian Deadlift",      3, "8"),
                ProgramExercise("Leg Press",              3, "10"),
                ProgramExercise("Leg Curl",               3, "10-12"),
                ProgramExercise("Calf Raise",             4, "15")
            )),
            ProgramDay(3, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(4, "Upper B", "Volume Focus", listOf(
                ProgramExercise("Incline Bench Press",    4, "10-12"),
                ProgramExercise("Dumbbell Row",           4, "10-12","Each arm"),
                ProgramExercise("Dumbbell Shoulder Press",3, "12-15"),
                ProgramExercise("Cable Row",              3, "12-15"),
                ProgramExercise("Lateral Raise",          3, "15-20"),
                ProgramExercise("Hammer Curl",            3, "12-15"),
                ProgramExercise("Skull Crusher",          3, "12-15")
            )),
            ProgramDay(5, "Lower B", "Volume Focus", listOf(
                ProgramExercise("Deadlift",               3, "5",    "Moderate weight"),
                ProgramExercise("Bulgarian Split Squat",  3, "10",   "Each leg"),
                ProgramExercise("Leg Extension",          3, "15-20"),
                ProgramExercise("Leg Curl",               3, "15-20"),
                ProgramExercise("Hip Thrust",             3, "12-15"),
                ProgramExercise("Calf Raise",             3, "20")
            )),
            ProgramDay(6, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    // ── POWERLIFTING ─────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "pl_531",
        name          = "5/3/1 Method",
        category      = ProgramCategory.POWERLIFTING,
        tagline       = "Jim Wendler's legendary strength program",
        description   = "Built around 4 main lifts: Squat, Bench, Deadlift, OHP. Each week uses percentage-based loading at 85/90/95% of your training max. Slow, consistent strength gains over months.",
        daysPerWeek   = 4,
        durationWeeks = 16,
        difficulty    = Difficulty.INTERMEDIATE,
        days          = listOf(
            ProgramDay(1, "Press Day", "Overhead Press Focus", listOf(
                ProgramExercise("Overhead Press",         3, "5/3/1","Work up to top set"),
                ProgramExercise("Bench Press",            5, "10",   "50-60% of max"),
                ProgramExercise("Dumbbell Row",           5, "10",   "Each arm"),
                ProgramExercise("Chin-up",                3, "AMRAP","Bodyweight"),
                ProgramExercise("Tricep Extension",       3, "15"),
                ProgramExercise("Bicep Curl",             3, "15")
            )),
            ProgramDay(2, "Deadlift Day", "Hip Hinge Focus", listOf(
                ProgramExercise("Deadlift",               3, "5/3/1","Work up to top set"),
                ProgramExercise("Romanian Deadlift",      4, "8",    "60% of deadlift max"),
                ProgramExercise("Leg Curl",               3, "10-12"),
                ProgramExercise("Plank",                  3, "60 sec"),
                ProgramExercise("Russian Twist",          3, "20"),
                ProgramExercise("Leg Raise",              3, "15")
            )),
            ProgramDay(3, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(4, "Bench Day", "Horizontal Press", listOf(
                ProgramExercise("Bench Press",            3, "5/3/1","Work up to top set"),
                ProgramExercise("Overhead Press",         5, "10",   "50-60% of max"),
                ProgramExercise("Dumbbell Row",           5, "10",   "Each arm"),
                ProgramExercise("Dumbbell Fly",           3, "12-15"),
                ProgramExercise("Lat Pulldown",           3, "10-12"),
                ProgramExercise("Face Pull",              3, "20")
            )),
            ProgramDay(5, "Squat Day", "Knee Dominant", listOf(
                ProgramExercise("Squat",                  3, "5/3/1","Work up to top set"),
                ProgramExercise("Leg Press",              5, "10",   "60% effort"),
                ProgramExercise("Leg Extension",          3, "15"),
                ProgramExercise("Leg Curl",               3, "10-12"),
                ProgramExercise("Calf Raise",             4, "15"),
                ProgramExercise("Plank",                  3, "60 sec")
            )),
            ProgramDay(6, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    ProgramTemplate(
        id            = "pl_sheiko",
        name          = "Sheiko-Style",
        category      = ProgramCategory.POWERLIFTING,
        tagline       = "High-frequency squat, bench & deadlift",
        description   = "Russian powerlifting methodology — high frequency on the competition lifts with moderate weights. Each session touches squat, bench, and deadlift. Builds technical consistency and raw strength.",
        daysPerWeek   = 4,
        durationWeeks = 12,
        difficulty    = Difficulty.ADVANCED,
        days          = listOf(
            ProgramDay(1, "Day 1", "Squat + Bench Heavy", listOf(
                ProgramExercise("Squat",                  5, "5",    "70-75% of max"),
                ProgramExercise("Bench Press",            5, "5",    "70-75% of max"),
                ProgramExercise("Romanian Deadlift",      3, "8"),
                ProgramExercise("Barbell Row",            3, "8")
            )),
            ProgramDay(2, "Day 2", "Deadlift + Bench Volume", listOf(
                ProgramExercise("Deadlift",               4, "4",    "72-78% of max"),
                ProgramExercise("Bench Press",            4, "6",    "65-70% of max"),
                ProgramExercise("Squat",                  3, "5",    "65% — technique"),
                ProgramExercise("Face Pull",              3, "15")
            )),
            ProgramDay(3, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(4, "Day 3", "Bench Heavy + Squat", listOf(
                ProgramExercise("Bench Press",            6, "4",    "75-80% of max"),
                ProgramExercise("Squat",                  4, "5",    "72% of max"),
                ProgramExercise("Leg Curl",               3, "10"),
                ProgramExercise("Tricep Extension",       3, "12")
            )),
            ProgramDay(5, "Day 4", "Deadlift Heavy + Bench", listOf(
                ProgramExercise("Deadlift",               5, "3",    "80-85% — heavy"),
                ProgramExercise("Bench Press",            3, "5",    "70% — speed"),
                ProgramExercise("Bulgarian Split Squat",  3, "8",    "Each leg"),
                ProgramExercise("Lat Pulldown",           3, "10")
            )),
            ProgramDay(6, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    // ── HYROX ────────────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "hyrox_prep",
        name          = "Hyrox Race Prep",
        category      = ProgramCategory.HYROX,
        tagline       = "8 functional stations + running",
        description   = "Hyrox consists of 8x 1km runs each followed by a functional exercise station. This program builds both your aerobic base and station-specific strength. Prep for race day in 12 weeks.",
        daysPerWeek   = 5,
        durationWeeks = 12,
        difficulty    = Difficulty.INTERMEDIATE,
        days          = listOf(
            ProgramDay(1, "Station Strength", "Functional Power", listOf(
                ProgramExercise("Rowing",                 4, "500m", "Build pace"),
                ProgramExercise("Burpee Broad Jump",      4, "10",   "Explosive"),
                ProgramExercise("Farmers Walk",           4, "20m",  "Heavy weight"),
                ProgramExercise("Sandbag Lunges",         4, "20m",  "Or dumbbell lunges"),
                ProgramExercise("Wall Ball",              4, "20",   "Use 6-9 kg ball"),
                ProgramExercise("Plank",                  3, "60 sec")
            ), cardioNotes = "10 min easy run warm-up"),
            ProgramDay(2, "Zone 2 Run", "Aerobic Base", emptyList(), isRestDay = false,
                cardioNotes = "40-50 min easy run at conversational pace (Zone 2). Heart rate 120-140 bpm."),
            ProgramDay(3, "Strength + Ski Erg", "Upper & Core", listOf(
                ProgramExercise("Overhead Press",         4, "8-10"),
                ProgramExercise("Barbell Row",            4, "8-10"),
                ProgramExercise("Ski Erg",                5, "250m", "Or cable pulls"),
                ProgramExercise("Push-up",                3, "AMRAP"),
                ProgramExercise("Pull-up",                3, "AMRAP"),
                ProgramExercise("Ab Wheel Rollout",       3, "10-12")
            )),
            ProgramDay(4, "Intervals", "Speed Work", emptyList(), isRestDay = false,
                cardioNotes = "8x 400m intervals at 85-90% effort. 90 sec rest between. Then 10 min easy cool down."),
            ProgramDay(5, "Leg Strength", "Lower Power", listOf(
                ProgramExercise("Squat",                  4, "5",    "Heavy"),
                ProgramExercise("Leg Press",              3, "12"),
                ProgramExercise("Romanian Deadlift",      3, "10"),
                ProgramExercise("Lunge",                  3, "20",   "Each leg — walking"),
                ProgramExercise("Calf Raise",             4, "15"),
                ProgramExercise("Hip Thrust",             3, "12")
            )),
            ProgramDay(6, "Long Run", "Endurance", emptyList(), isRestDay = false,
                cardioNotes = "60-90 min long slow run. Keep it easy — this builds your aerobic engine."),
            ProgramDay(7, "Rest", "Active Recovery", emptyList(), isRestDay = true)
        )
    ),

    // ── RUNNING ──────────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "running_5k",
        name          = "5K Runner",
        category      = ProgramCategory.RUNNING,
        tagline       = "Couch to sub-25 minute 5K",
        description   = "A 10-week progressive running plan combining easy runs, intervals, and a weekly long run. Includes strength work to prevent injury. Perfect for beginners or those returning to running.",
        daysPerWeek   = 5,
        durationWeeks = 10,
        difficulty    = Difficulty.BEGINNER,
        days          = listOf(
            ProgramDay(1, "Easy Run", "Aerobic Base", emptyList(), isRestDay = false,
                cardioNotes = "20-30 min easy run. Should be able to hold a conversation."),
            ProgramDay(2, "Strength", "Injury Prevention", listOf(
                ProgramExercise("Squat",                  3, "10"),
                ProgramExercise("Romanian Deadlift",      3, "10"),
                ProgramExercise("Hip Thrust",             3, "12"),
                ProgramExercise("Lunge",                  3, "10",   "Each leg"),
                ProgramExercise("Calf Raise",             4, "15"),
                ProgramExercise("Plank",                  3, "45 sec")
            )),
            ProgramDay(3, "Intervals", "Speed", emptyList(), isRestDay = false,
                cardioNotes = "6x 400m at 5K goal pace. Rest 90 sec between. Warm up 10 min easy beforehand."),
            ProgramDay(4, "Rest / Walk", "Recovery", emptyList(), isRestDay = true,
                cardioNotes = "Optional 20 min walk for active recovery."),
            ProgramDay(5, "Tempo Run", "Lactate Threshold", emptyList(), isRestDay = false,
                cardioNotes = "5 min easy, 15 min at comfortably hard pace (you can speak 3-4 words), 5 min easy."),
            ProgramDay(6, "Long Run", "Endurance", emptyList(), isRestDay = false,
                cardioNotes = "30-45 min long slow run. Builds your base. Slower than easy pace is fine."),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    ProgramTemplate(
        id            = "running_10k",
        name          = "10K Builder",
        category      = ProgramCategory.RUNNING,
        tagline       = "Build to a strong 10K race",
        description   = "12-week plan for runners who can already run 5K comfortably. Adds mileage progressively with interval sessions, tempo runs, and strength training to hit a 10K personal best.",
        daysPerWeek   = 5,
        durationWeeks = 12,
        difficulty    = Difficulty.INTERMEDIATE,
        days          = listOf(
            ProgramDay(1, "Easy Run", "Zone 2", emptyList(), isRestDay = false,
                cardioNotes = "35-40 min easy. Heart rate under 140 bpm."),
            ProgramDay(2, "Strength", "Runner Strength", listOf(
                ProgramExercise("Deadlift",               3, "6",    "Moderate weight"),
                ProgramExercise("Bulgarian Split Squat",  3, "8",    "Each leg"),
                ProgramExercise("Hip Thrust",             3, "12"),
                ProgramExercise("Lateral Raise",          3, "15"),
                ProgramExercise("Calf Raise",             4, "20"),
                ProgramExercise("Plank",                  3, "60 sec")
            )),
            ProgramDay(3, "Intervals", "Speed Work", emptyList(), isRestDay = false,
                cardioNotes = "10x 400m at 10K goal pace –5 sec. 75 sec recovery jog. Warm/cool 10 min each."),
            ProgramDay(4, "Easy Run", "Recovery Run", emptyList(), isRestDay = false,
                cardioNotes = "25-30 min very easy. Flush the legs out."),
            ProgramDay(5, "Tempo", "Race Pace Work", emptyList(), isRestDay = false,
                cardioNotes = "10 min easy, 20 min at 10K goal race pace, 10 min easy."),
            ProgramDay(6, "Long Run", "Distance Build", emptyList(), isRestDay = false,
                cardioNotes = "50-70 min long easy run. Add 5 min each week."),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    // ── STRONGMAN ────────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "strongman_events",
        name          = "Strongman Foundation",
        category      = ProgramCategory.STRONGMAN,
        tagline       = "Log press, atlas stones, farmer's walk",
        description   = "Train like a strongman with event-specific movements: log press, atlas stone, farmer's carry, yoke walk, and tire flip. Paired with heavy compound lifts for raw power development.",
        daysPerWeek   = 4,
        durationWeeks = 12,
        difficulty    = Difficulty.INTERMEDIATE,
        days          = listOf(
            ProgramDay(1, "Push & Log Press", "Overhead Power", listOf(
                ProgramExercise("Log Press",              5, "5",    "Or use barbell if no log"),
                ProgramExercise("Overhead Press",         3, "8"),
                ProgramExercise("Incline Bench Press",    3, "8-10"),
                ProgramExercise("Tricep Extension",       3, "12"),
                ProgramExercise("Lateral Raise",          3, "15"),
                ProgramExercise("Push-up",                3, "AMRAP","Burnout")
            )),
            ProgramDay(2, "Deadlift & Events", "Pull & Carry", listOf(
                ProgramExercise("Deadlift",               5, "3-5",  "Work up to heavy triple"),
                ProgramExercise("Farmers Walk",           4, "30m",  "Heavy — event simulation"),
                ProgramExercise("Barbell Row",            3, "8"),
                ProgramExercise("Shrug",                  4, "10-12","Heavy"),
                ProgramExercise("Lat Pulldown",           3, "10"),
                ProgramExercise("Chin-up",                3, "AMRAP")
            )),
            ProgramDay(3, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(4, "Squat & Yoke", "Leg Power", listOf(
                ProgramExercise("Squat",                  5, "5",    "Build to 5RM"),
                ProgramExercise("Yoke Walk",              4, "20m",  "Or heavy barbell carry"),
                ProgramExercise("Romanian Deadlift",      3, "8-10"),
                ProgramExercise("Leg Press",              3, "10-12"),
                ProgramExercise("Calf Raise",             4, "15"),
                ProgramExercise("Hip Thrust",             3, "10")
            )),
            ProgramDay(5, "Atlas Stone / Medley", "Event Day", listOf(
                ProgramExercise("Atlas Stone",            5, "5",    "Or sandbag load-over"),
                ProgramExercise("Sandbag Carry",          4, "30m",  "Over shoulder or bear hug"),
                ProgramExercise("Tire Flip",              4, "8",    "Or heavy hex deadlift"),
                ProgramExercise("Battle Ropes",           3, "30 sec","Or sled push"),
                ProgramExercise("Ab Wheel Rollout",       3, "10-12")
            )),
            ProgramDay(6, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    ),

    // ── CALISTHENICS ─────────────────────────────────────────────────────────

    ProgramTemplate(
        id            = "calisthenics_foundation",
        name          = "Calisthenics Foundation",
        category      = ProgramCategory.CALISTHENICS,
        tagline       = "Master your bodyweight from scratch",
        description   = "Build real strength using only your bodyweight. Progress from basic movements toward pull-up, dip, push-up variations and core control. No gym required — just a pull-up bar.",
        daysPerWeek   = 4,
        durationWeeks = 10,
        difficulty    = Difficulty.BEGINNER,
        days          = listOf(
            ProgramDay(1, "Push", "Chest & Triceps", listOf(
                ProgramExercise("Push-up",                4, "AMRAP","Full range of motion"),
                ProgramExercise("Incline Push-up",        3, "15",   "Easier variation"),
                ProgramExercise("Dip",                    3, "AMRAP","Use parallel bars or chair"),
                ProgramExercise("Pike Push-up",           3, "10",   "Shoulder work"),
                ProgramExercise("Tricep Extension",       3, "12",   "Use bodyweight on floor"),
                ProgramExercise("Plank",                  3, "60 sec")
            )),
            ProgramDay(2, "Pull", "Back & Biceps", listOf(
                ProgramExercise("Pull-up",                4, "AMRAP","Jump up and lower slowly"),
                ProgramExercise("Chin-up",                3, "AMRAP"),
                ProgramExercise("Inverted Row",           3, "10-15","Under a table or bar"),
                ProgramExercise("Face Pull",              3, "15",   "Use resistance band"),
                ProgramExercise("Bicep Curl",             3, "12",   "Use resistance band"),
                ProgramExercise("Hanging Knee Raise",     3, "12")
            )),
            ProgramDay(3, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(4, "Legs", "Lower Body", listOf(
                ProgramExercise("Squat",                  4, "20",   "Bodyweight — full depth"),
                ProgramExercise("Lunge",                  3, "15",   "Each leg"),
                ProgramExercise("Hip Thrust",             3, "20",   "Bodyweight glute bridge"),
                ProgramExercise("Calf Raise",             4, "25",   "Single leg if easy"),
                ProgramExercise("Mountain Climber",       3, "30 sec"),
                ProgramExercise("Jump Rope",              3, "60 sec","Or jump in place")
            )),
            ProgramDay(5, "Full Body + Core", "Endurance", listOf(
                ProgramExercise("Push-up",                3, "15"),
                ProgramExercise("Pull-up",                3, "AMRAP"),
                ProgramExercise("Squat",                  3, "20"),
                ProgramExercise("Plank",                  4, "60 sec"),
                ProgramExercise("Russian Twist",          3, "20"),
                ProgramExercise("Leg Raise",              3, "15"),
                ProgramExercise("Crunch",                 3, "20")
            )),
            ProgramDay(6, "Rest", "Recovery", emptyList(), isRestDay = true),
            ProgramDay(7, "Rest", "Recovery", emptyList(), isRestDay = true)
        )
    )
)
