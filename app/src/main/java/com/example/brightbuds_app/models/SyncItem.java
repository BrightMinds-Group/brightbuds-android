package com.example.brightbuds_app.models;

public class SyncItem {
    private String id;
    private String tableName;
    private String recordId;
    private String operation;

    public SyncItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getParentId() {
        return "";
    }

    public String getChildId() {
        return "";
    }

    public String getModuleId() {
        return "";
    }

    public double getScore() {
        return 0;
    }

    public String getStatus() {
        return "";
    }

    public long getTimeSpent() {
        return 0;
    }
}
