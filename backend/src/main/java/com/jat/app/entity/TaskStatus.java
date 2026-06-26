package com.jat.app.entity;

// Status tracks workflow state independently from type so ideas, actions, and routines can all be completed or archived.
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}
