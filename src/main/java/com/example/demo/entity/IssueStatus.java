package com.example.demo.entity;

public enum IssueStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean isDoneCategory() {
        return this == DONE || this == CANCELLED;
    }
}
