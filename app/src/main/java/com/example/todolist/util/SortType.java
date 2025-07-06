package com.example.todolist.util;

public enum SortType {
    DATE_TIME("date_time"),
    CREATION_TIME("creation_time"),
    ALPHABETICAL("alphabetical");

    private final String value;

    SortType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SortType fromValue(String value) {
        for (SortType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return DATE_TIME; // Default
    }
}
