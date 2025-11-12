package com.example.brightbuds_app.models;

import com.example.brightbuds_app.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Progress model for child learning activity tracking.
 * Includes flexible timestamp handling and completion checks.
 */
public class Progress {

    private String progressId;
    private String parentId;
    private String childId;
    private String moduleId;
    private String status;
    private double score;
    private long timeSpent;
    private long timestamp;

    private int plays;
    private String type;
    private boolean completionStatus;

    private Object lastUpdated;

    public Progress() {}

    @PropertyName("progressId")
    public String getProgressId() { return progressId; }
    @PropertyName("progressId")
    public void setProgressId(String progressId) { this.progressId = progressId; }

    @PropertyName("parentId")
    public String getParentId() { return parentId; }
    @PropertyName("parentId")
    public void setParentId(String parentId) { this.parentId = parentId; }

    @PropertyName("childId")
    public String getChildId() { return childId; }
    @PropertyName("childId")
    public void setChildId(String childId) { this.childId = childId; }

    @PropertyName("moduleId")
    public String getModuleId() { return moduleId; }
    @PropertyName("moduleId")
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("score")
    public double getScore() { return score; }
    @PropertyName("score")
    public void setScore(double score) { this.score = score; }

    @PropertyName("timeSpent")
    public long getTimeSpent() { return timeSpent; }
    @PropertyName("timeSpent")
    public void setTimeSpent(long timeSpent) { this.timeSpent = timeSpent; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("plays")
    public int getPlays() { return plays; }
    @PropertyName("plays")
    public void setPlays(int plays) { this.plays = plays; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("completionStatus")
    public boolean isCompletionStatus() { return completionStatus; }
    @PropertyName("completionStatus")
    public void setCompletionStatus(boolean completionStatus) { this.completionStatus = completionStatus; }

    @PropertyName("lastUpdated")
    public Object getLastUpdated() { return lastUpdated; }
    @PropertyName("lastUpdated")
    public void setLastUpdated(Object lastUpdated) { this.lastUpdated = lastUpdated; }

    public Timestamp getLastUpdatedTimestamp() {
        if (lastUpdated instanceof Timestamp) return (Timestamp) lastUpdated;
        if (lastUpdated instanceof Long) return new Timestamp(((Long) lastUpdated) / 1000, 0);
        return null;
    }

    /**
     * Completion rules:
     * 1) completionStatus true
     * 2) score >= 70
     * 3) status equals "completed"
     */
    public boolean isModuleCompleted() {
        if (completionStatus) return true;
        if (score >= 70) return true;
        return status != null && status.equalsIgnoreCase("completed");
    }

    /** Display name resolved via Constants to support legacy and new ids */
    public String getModuleName() {
        return Constants.getModuleDisplayName(moduleId);
    }

    @Override
    public String toString() {
        return "Progress{" +
                "childId='" + childId + '\'' +
                ", moduleId='" + moduleId + '\'' +
                ", moduleName='" + getModuleName() + '\'' +
                ", plays=" + plays +
                ", score=" + score +
                ", completed=" + isModuleCompleted() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
