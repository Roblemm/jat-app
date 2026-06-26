package com.jat.app.entity;

// Keep status separate from goal type so any measurement style can be paused, completed, or archived.
public enum GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}
