package com.example.brightbuds_app.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

/**
 * UsageReport
 * Comprehensive model for learning analytics and system insights in BrightBuds.
 * Supports both Parent and Admin-level reports (weekly, monthly, and milestone-based).
 */
public class UsageReport {

    // --- Basic Report Metadata ---
    private String reportId;
    private String childId;
    private String childName;
    private String reportType;   // "weekly", "monthly", "quarterly", "milestone"
    private String period;       // e.g., "January 2024", "Week of Jan 1-7"
    private Timestamp generatedAt;
    private Timestamp periodStart;
    private Timestamp periodEnd;

    // --- Summary Metrics ---
    private double overallAverageScore;
    private int totalLearningTime;       // in minutes
    private int totalGamesCompleted;
    private int totalSessions;
    private int daysActive;
    private double engagementScore;

    // --- Module Performance ---
    private List<Progress> moduleBreakdown;
    private Map<String, Double> moduleScores;     // moduleId -> average score
    private Map<String, Integer> moduleTimeSpent; // moduleId -> minutes spent

    // --- Progress Analytics ---
    private double progressRate;
    private String strongestModule;
    private String improvementArea;
    private int streakDays;
    private boolean newMilestonesAchieved;
    private List<String> milestones;

    // --- Recommendations ---
    private String learningRecommendation;
    private List<String> suggestedModules;
    private List<String> focusAreas;

    // --- Admin Analytics ---
    private int newUsersThisPeriod;
    private double systemEngagementRate;
    private Map<String, Integer> modulePopularity;
    private List<String> topPerformingModules;
    private List<String> trendingInsights;

    /** Empty constructor REQUIRED for Firestore */
    public UsageReport() {}

    /** Constructor for report initialization */
    public UsageReport(String reportId, String childId, String childName, String reportType, String period) {
        this.reportId = reportId;
        this.childId = childId;
        this.childName = childName;
        this.reportType = reportType;
        this.period = period;
        this.generatedAt = new Timestamp(new java.util.Date());
        this.overallAverageScore = 0.0;
        this.totalLearningTime = 0;
        this.totalGamesCompleted = 0;
        this.totalSessions = 0;
        this.daysActive = 0;
        this.engagementScore = 0.0;
        this.progressRate = 0.0;
        this.streakDays = 0;
        this.newMilestonesAchieved = false;
    }

    // --- Firestore Mapped Getters and Setters ---

    @PropertyName("report_id")
    public String getReportId() { return reportId; }
    @PropertyName("report_id")
    public void setReportId(String reportId) { this.reportId = reportId; }

    @PropertyName("child_id")
    public String getChildId() { return childId; }
    @PropertyName("child_id")
    public void setChildId(String childId) { this.childId = childId; }

    @PropertyName("child_name")
    public String getChildName() { return childName; }
    @PropertyName("child_name")
    public void setChildName(String childName) { this.childName = childName; }

    @PropertyName("report_type")
    public String getReportType() { return reportType; }
    @PropertyName("report_type")
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    @PropertyName("generated_at")
    public Timestamp getGeneratedAt() { return generatedAt; }
    @PropertyName("generated_at")
    public void setGeneratedAt(Timestamp generatedAt) { this.generatedAt = generatedAt; }

    @PropertyName("period_start")
    public Timestamp getPeriodStart() { return periodStart; }
    @PropertyName("period_start")
    public void setPeriodStart(Timestamp periodStart) { this.periodStart = periodStart; }

    @PropertyName("period_end")
    public Timestamp getPeriodEnd() { return periodEnd; }
    @PropertyName("period_end")
    public void setPeriodEnd(Timestamp periodEnd) { this.periodEnd = periodEnd; }

    @PropertyName("overall_average_score")
    public double getOverallAverageScore() { return overallAverageScore; }
    @PropertyName("overall_average_score")
    public void setOverallAverageScore(double overallAverageScore) { this.overallAverageScore = overallAverageScore; }

    @PropertyName("total_learning_time")
    public int getTotalLearningTime() { return totalLearningTime; }
    @PropertyName("total_learning_time")
    public void setTotalLearningTime(int totalLearningTime) { this.totalLearningTime = totalLearningTime; }

    @PropertyName("total_games_completed")
    public int getTotalGamesCompleted() { return totalGamesCompleted; }
    @PropertyName("total_games_completed")
    public void setTotalGamesCompleted(int totalGamesCompleted) { this.totalGamesCompleted = totalGamesCompleted; }

    @PropertyName("total_sessions")
    public int getTotalSessions() { return totalSessions; }
    @PropertyName("total_sessions")
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    @PropertyName("days_active")
    public int getDaysActive() { return daysActive; }
    @PropertyName("days_active")
    public void setDaysActive(int daysActive) { this.daysActive = daysActive; }

    @PropertyName("engagement_score")
    public double getEngagementScore() { return engagementScore; }
    @PropertyName("engagement_score")
    public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }

    @PropertyName("module_breakdown")
    public List<Progress> getModuleBreakdown() { return moduleBreakdown; }
    @PropertyName("module_breakdown")
    public void setModuleBreakdown(List<Progress> moduleBreakdown) { this.moduleBreakdown = moduleBreakdown; }

    @PropertyName("module_scores")
    public Map<String, Double> getModuleScores() { return moduleScores; }
    @PropertyName("module_scores")
    public void setModuleScores(Map<String, Double> moduleScores) { this.moduleScores = moduleScores; }

    @PropertyName("module_time_spent")
    public Map<String, Integer> getModuleTimeSpent() { return moduleTimeSpent; }
    @PropertyName("module_time_spent")
    public void setModuleTimeSpent(Map<String, Integer> moduleTimeSpent) { this.moduleTimeSpent = moduleTimeSpent; }

    @PropertyName("progress_rate")
    public double getProgressRate() { return progressRate; }
    @PropertyName("progress_rate")
    public void setProgressRate(double progressRate) { this.progressRate = progressRate; }

    @PropertyName("strongest_module")
    public String getStrongestModule() { return strongestModule; }
    @PropertyName("strongest_module")
    public void setStrongestModule(String strongestModule) { this.strongestModule = strongestModule; }

    @PropertyName("improvement_area")
    public String getImprovementArea() { return improvementArea; }
    @PropertyName("improvement_area")
    public void setImprovementArea(String improvementArea) { this.improvementArea = improvementArea; }

    @PropertyName("streak_days")
    public int getStreakDays() { return streakDays; }
    @PropertyName("streak_days")
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    @PropertyName("new_milestones_achieved")
    public boolean isNewMilestonesAchieved() { return newMilestonesAchieved; }
    @PropertyName("new_milestones_achieved")
    public void setNewMilestonesAchieved(boolean newMilestonesAchieved) { this.newMilestonesAchieved = newMilestonesAchieved; }

    public List<String> getMilestones() { return milestones; }
    public void setMilestones(List<String> milestones) { this.milestones = milestones; }

    @PropertyName("learning_recommendation")
    public String getLearningRecommendation() { return learningRecommendation; }
    @PropertyName("learning_recommendation")
    public void setLearningRecommendation(String learningRecommendation) { this.learningRecommendation = learningRecommendation; }

    @PropertyName("suggested_modules")
    public List<String> getSuggestedModules() { return suggestedModules; }
    @PropertyName("suggested_modules")
    public void setSuggestedModules(List<String> suggestedModules) { this.suggestedModules = suggestedModules; }

    @PropertyName("focus_areas")
    public List<String> getFocusAreas() { return focusAreas; }
    @PropertyName("focus_areas")
    public void setFocusAreas(List<String> focusAreas) { this.focusAreas = focusAreas; }

    // --- Admin-specific Getters/Setters ---

    @PropertyName("new_users_this_period")
    public int getNewUsersThisPeriod() { return newUsersThisPeriod; }
    @PropertyName("new_users_this_period")
    public void setNewUsersThisPeriod(int newUsersThisPeriod) { this.newUsersThisPeriod = newUsersThisPeriod; }

    @PropertyName("system_engagement_rate")
    public double getSystemEngagementRate() { return systemEngagementRate; }
    @PropertyName("system_engagement_rate")
    public void setSystemEngagementRate(double systemEngagementRate) { this.systemEngagementRate = systemEngagementRate; }

    @PropertyName("module_popularity")
    public Map<String, Integer> getModulePopularity() { return modulePopularity; }
    @PropertyName("module_popularity")
    public void setModulePopularity(Map<String, Integer> modulePopularity) { this.modulePopularity = modulePopularity; }

    @PropertyName("top_performing_modules")
    public List<String> getTopPerformingModules() { return topPerformingModules; }
    @PropertyName("top_performing_modules")
    public void setTopPerformingModules(List<String> topPerformingModules) { this.topPerformingModules = topPerformingModules; }

    @PropertyName("trending_insights")
    public List<String> getTrendingInsights() { return trendingInsights; }
    @PropertyName("trending_insights")
    public void setTrendingInsights(List<String> trendingInsights) { this.trendingInsights = trendingInsights; }

    // --- Business Logic and Helper Methods ---

    /** Returns formatted report title based on type */
    public String getReportTitle() {
        switch (reportType) {
            case "weekly": return "Weekly Progress Report";
            case "monthly": return "Monthly Progress Report";
            case "quarterly": return "Quarterly Progress Report";
            case "milestone": return "Milestone Achievement Report";
            default: return "Progress Report";
        }
    }

    /** Converts total learning time into readable format */
    public String getTimeSpentFormatted() {
        int hours = totalLearningTime / 60;
        int minutes = totalLearningTime % 60;
        return (hours > 0)
                ? String.format("%d hours %d minutes", hours, minutes)
                : String.format("%d minutes", minutes);
    }

    /** Returns average session duration */
    public double getAverageSessionTime() {
        return totalSessions > 0 ? (double) totalLearningTime / totalSessions : 0.0;
    }

    /** Interprets engagement score into a descriptive level */
    public String getEngagementLevel() {
        if (engagementScore >= 90) return "Very High";
        else if (engagementScore >= 70) return "High";
        else if (engagementScore >= 50) return "Moderate";
        else return "Low";
    }

    /** True if progress rate shows any improvement */
    public boolean showsImprovement() {
        return progressRate > 0;
    }

    /** Returns descriptive progress trend label */
    public String getProgressTrend() {
        if (progressRate > 10) return "Rapid Improvement";
        else if (progressRate > 5) return "Steady Improvement";
        else if (progressRate > 0) return "Slight Improvement";
        else if (progressRate == 0) return "Maintaining";
        else return "Needs Attention";
    }

    /** Calculates overall engagement score (0–100) */
    public void calculateEngagementScore() {
        double sessionScore = Math.min(totalSessions * 10, 40);
        double timeScore = Math.min(totalLearningTime / 10.0, 30);
        double consistencyScore = Math.min(daysActive * 5, 30);
        engagementScore = sessionScore + timeScore + consistencyScore;
    }

    @Override
    public String toString() {
        return "UsageReport{" +
                "reportType='" + reportType + '\'' +
                ", period='" + period + '\'' +
                ", overallAverageScore=" + overallAverageScore +
                ", totalLearningTime=" + totalLearningTime +
                ", totalGamesCompleted=" + totalGamesCompleted +
                ", engagementScore=" + engagementScore +
                '}';
    }
}
