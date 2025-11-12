package com.example.brightbuds_app.utils;

import com.example.brightbuds_app.BuildConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 Centralized constants and configuration for the BrightBuds application.
 All static configuration values, limits, and display parameters are defined here.
 This class should never be instantiated.
*/
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Constants class should not be instantiated");
    }

    // APP CONFIGURATION
    public static final String APP_NAME = "BrightBuds";
    public static final String APP_VERSION = "1.0.0";
    public static final int APP_VERSION_CODE = 1;
    public static final boolean IS_DEBUG = BuildConfig.DEBUG;

    // FIREBASE CONFIGURATION
    public static final String FIREBASE_PROJECT_ID = "brightbuds-app";
    public static final long STORAGE_MAX_RETRY_TIME = 60000L;
    public static final int FIRESTORE_MAX_RETRIES = 3;
    public static final long FIRESTORE_TIMEOUT_MS = 10000L;

    // DATABASE COLLECTION NAMES
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CHILDREN = "children";
    public static final String COLLECTION_PROGRESS = "progress";
    public static final String COLLECTION_GAME_MODULES = "game_modules";
    public static final String COLLECTION_REPORTS = "reports";
    public static final String COLLECTION_SETTINGS = "settings";

    // GAME MODULE IDS (existing)
    public static final String MODULE_ABC_SONG = "module_abc_song";
    public static final String MODULE_123_SONG = "module_123_song";
    public static final String MODULE_FAMILY = "module_family";
    public static final String GAME_FEED_MONSTER = "game_feed_monster";
    public static final String GAME_WORD_BUILDER = "game_word_builder";
    public static final String GAME_MATCH_LETTER = "game_match_letter";
    public static final String GAME_MEMORY_MATCH = "game_memory_match";

    // NEW MODULE ID
    public static final String GAME_SHAPES_MATCH = "game_shapes_match";

    // MODULE TYPES
    public static final String TYPE_SONG = "song";
    public static final String TYPE_GAME = "game";
    public static final String TYPE_FAMILY = "family";
    public static final String TYPE_INTERACTIVE = "interactive";

    // LEARNING CATEGORIES
    public static final String CATEGORY_LITERACY = "literacy";
    public static final String CATEGORY_NUMERACY = "numeracy";
    public static final String CATEGORY_COGNITIVE = "cognitive";
    public static final String CATEGORY_SOCIAL = "social";

    // DIFFICULTY LEVELS
    public static final String DIFFICULTY_BEGINNER = "beginner";
    public static final String DIFFICULTY_INTERMEDIATE = "intermediate";
    public static final String DIFFICULTY_ADVANCED = "advanced";

    // COMPLETION STATUS
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_NOT_STARTED = "not_started";
    public static final String STATUS_ABANDONED = "abandoned";

    // PERFORMANCE LEVELS
    public static final String PERFORMANCE_EXCELLENT = "excellent";
    public static final String PERFORMANCE_GOOD = "good";
    public static final String PERFORMANCE_IMPROVING = "improving";
    public static final String PERFORMANCE_NEEDS_IMPROVEMENT = "needs_improvement";

    // LEARNING LEVELS
    public static final String LEVEL_NEW_LEARNER = "new_learner";
    public static final String LEVEL_BEGINNER = "beginner";
    public static final String LEVEL_INTERMEDIATE = "intermediate";
    public static final String LEVEL_ADVANCED = "advanced";

    // REPORT TYPES
    public static final String REPORT_WEEKLY = "weekly";
    public static final String REPORT_MONTHLY = "monthly";
    public static final String REPORT_QUARTERLY = "quarterly";
    public static final String REPORT_MILESTONE = "milestone";
    public static final String REPORT_ADMIN = "admin";

    // REPORT TRIGGERS
    public static final String TRIGGER_SCHEDULED = "scheduled";
    public static final String TRIGGER_GAME_COMPLETION = "game_completion";
    public static final String TRIGGER_MILESTONE = "milestone";
    public static final String TRIGGER_SYSTEM_EVENT = "system_event";

    // AUTOMATIC REPORT SETTINGS
    public static final boolean AUTO_GENERATE_REPORTS = true;
    public static final String DEFAULT_REPORT_SCHEDULE = REPORT_WEEKLY;
    public static final int MILESTONE_GAMES_COMPLETED = 10;
    public static final int AUTO_REPORT_MIN_SCORE = 80;

    // USER ROLES
    public static final String ROLE_PARENT = "parent";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_DEVELOPER = "developer";

    // AGE GROUPS
    public static final int AGE_GROUP_TODDLER_MIN = 2;
    public static final int AGE_GROUP_TODDLER_MAX = 3;
    public static final int AGE_GROUP_PRESCHOOLER_MIN = 4;
    public static final int AGE_GROUP_PRESCHOOLER_MAX = 5;
    public static final int AGE_GROUP_KINDERGARTEN_MIN = 6;
    public static final int AGE_GROUP_KINDERGARTEN_MAX = 7;

    // LIMITS AND CONSTRAINTS
    public static final int MAX_CHILDREN_PER_PARENT = 5;
    public static final int MAX_CUSTOM_WORDS = 4;
    public static final int MAX_FAMILY_PHOTOS = 10;
    public static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_SESSION_TIME_MINUTES = 30;
    public static final int DAILY_SESSION_LIMIT = 5;

    // SCORING AND PERFORMANCE
    public static final int PERFECT_SCORE = 100;
    public static final int EXCELLENT_SCORE_THRESHOLD = 90;
    public static final int GOOD_SCORE_THRESHOLD = 75;
    public static final int PASSING_SCORE_THRESHOLD = 60;
    public static final int MASTERY_SESSIONS_REQUIRED = 5;
    public static final double MASTERY_SCORE_THRESHOLD = 85.0;

    // TIME CONSTANTS
    public static final long ONE_SECOND_MS = 1000L;
    public static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    public static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    public static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    public static final long ONE_WEEK_MS = 7 * ONE_DAY_MS;
    public static final long ONE_MONTH_MS = 30 * ONE_DAY_MS;

    // SYNC AND OFFLINE SETTINGS
    public static final long SYNC_INTERVAL_MS = 15 * ONE_MINUTE_MS;
    public static final long MAX_OFFLINE_STORAGE_DAYS = 30;
    public static final int MAX_SYNC_RETRIES = 3;
    public static final long SYNC_RETRY_DELAY_MS = 5 * ONE_SECOND_MS;

    // NOTIFICATION SETTINGS
    public static final String NOTIFICATION_CHANNEL_PROGRESS = "progress_updates";
    public static final String NOTIFICATION_CHANNEL_REPORTS = "report_notifications";
    public static final String NOTIFICATION_CHANNEL_SYSTEM = "system_notifications";

    public static final int NOTIFICATION_ID_PROGRESS = 1000;
    public static final int NOTIFICATION_ID_REPORT = 1001;
    public static final int NOTIFICATION_ID_SYSTEM = 1002;

    // PREFERENCE KEYS
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_USER_LOGGED_IN = "user_logged_in";
    public static final String PREF_LAST_SYNC_TIME = "last_sync_time";
    public static final String PREF_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    public static final String PREF_OFFLINE_MODE_ENABLED = "offline_mode_enabled";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PREF_SOUND_EFFECTS_ENABLED = "sound_effects_enabled";
    public static final String PREF_MUSIC_ENABLED = "music_enabled";
    public static final String PREF_LAST_REPORT_GENERATED = "last_report_generated";

    // ERROR CODES
    public static final String ERROR_NETWORK_UNAVAILABLE = "network_unavailable";
    public static final String ERROR_USER_NOT_LOGGED_IN = "user_not_logged_in";
    public static final String ERROR_INVALID_DATA = "invalid_data";
    public static final String ERROR_PERMISSION_DENIED = "permission_denied";
    public static final String ERROR_STORAGE_LIMIT_EXCEEDED = "storage_limit_exceeded";
    public static final String ERROR_CHILD_LIMIT_EXCEEDED = "child_limit_exceeded";

    // SUCCESS & USER MESSAGES
    public static final String MSG_NETWORK_ERROR = "Please check your internet connection and try again.";
    public static final String MSG_AUTH_ERROR = "Authentication failed. Please check your credentials.";
    public static final String MSG_DATA_ERROR = "There was a problem loading your data.";
    public static final String MSG_GENERIC_ERROR = "Something went wrong. Please try again.";
    public static final String MSG_OFFLINE_MODE = "You're currently offline. Some features may be limited.";
    public static final String MSG_SYNC_SUCCESS = "All data has been synchronized successfully.";
    public static final String MSG_CHILD_ADDED = "Child profile added successfully!";
    public static final String MSG_PROGRESS_SAVED = "Learning progress saved!";
    public static final String MSG_REPORT_GENERATED = "Report generated successfully!";
    public static final String MSG_SETTINGS_SAVED = "Settings updated successfully!";

    // REMOTE CONFIG KEYS
    public static final String REMOTE_AUTO_REPORT_ENABLED = "auto_report_enabled";
    public static final String REMOTE_DAILY_SESSION_LIMIT = "daily_session_limit";
    public static final String REMOTE_WEEKLY_REPORT_DAY = "weekly_report_day";
    public static final String REMOTE_SESSION_TIME_LIMIT = "session_time_limit";
    public static final String REMOTE_FEATURE_FLAG_FAMILY_MODULE = "feature_family_module";
    public static final String REMOTE_FEATURE_FLAG_ADVANCED_ANALYTICS = "feature_advanced_analytics";

    // REMOTE CONFIG DEFAULTS
    public static Map<String, Object> getRemoteConfigDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("auto_report_enabled", true);
        defaults.put("weekly_report_day", 0);
        defaults.put("session_time_limit", MAX_SESSION_TIME_MINUTES);
        defaults.put("daily_session_limit", DAILY_SESSION_LIMIT);
        defaults.put("feature_family_module", true);
        defaults.put("feature_advanced_analytics", false);
        return defaults;
    }

    // Return all BrightBuds module IDs
    public static String[] getAllGameModuleIds() {
        return new String[]{
                MODULE_ABC_SONG, MODULE_123_SONG, MODULE_FAMILY,
                GAME_FEED_MONSTER, GAME_WORD_BUILDER,
                GAME_MATCH_LETTER, GAME_MEMORY_MATCH,
                GAME_SHAPES_MATCH
        };
    }

    // Display name resolver that accepts legacy ids and new ids
    public static String getModuleDisplayName(String moduleId) {
        if (moduleId == null) return "Unknown Module";
        switch (moduleId) {
            // New ids
            case GAME_FEED_MONSTER: return "Feed the Monster";
            case GAME_WORD_BUILDER: return "Word Builder";
            case GAME_MATCH_LETTER: return "Match the Letter";
            case GAME_MEMORY_MATCH: return "Memory Match";
            case MODULE_ABC_SONG: return "ABC Song";
            case MODULE_123_SONG: return "123 Song";
            case MODULE_FAMILY: return "Family Photos";
            case GAME_SHAPES_MATCH: return "Shapes Match";
            // Legacy ids used by some components
            case "module_feed_the_monster": return "Feed the Monster";
            case "module_match_the_letter": return "Match the Letter";
            case "module_memory_match": return "Memory Match";
            case "module_word_builder": return "Word Builder";
            case "module_my_family": return "My Family Album";
            default: return "Unknown Module";
        }
    }

    // Determine performance level based on score
    public static String getPerformanceLevel(double score) {
        if (score >= EXCELLENT_SCORE_THRESHOLD) return PERFORMANCE_EXCELLENT;
        else if (score >= GOOD_SCORE_THRESHOLD) return PERFORMANCE_GOOD;
        else if (score >= PASSING_SCORE_THRESHOLD) return PERFORMANCE_IMPROVING;
        else return PERFORMANCE_NEEDS_IMPROVEMENT;
    }

    // Determine color associated with a given score
    public static String getPerformanceColor(double score) {
        if (score >= EXCELLENT_SCORE_THRESHOLD) return "#4CAF50";
        else if (score >= GOOD_SCORE_THRESHOLD) return "#8BC34A";
        else if (score >= PASSING_SCORE_THRESHOLD) return "#FFC107";
        else return "#F44336";
    }

    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return (minutes > 0)
                ? String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                : String.format(Locale.getDefault(), "%ds", seconds);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        else
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static boolean isMasteryScore(double score, int sessionsCompleted) {
        return score >= MASTERY_SCORE_THRESHOLD && sessionsCompleted >= MASTERY_SESSIONS_REQUIRED;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("^[a-zA-Z\\s]{2,50}$");
    }
}
