package com.example.todolist.model;

public class Category {
    private String name;
    private int taskCount;
    private String color;

    public Category(String name, int taskCount, String color) {
        this.name = name;
        this.taskCount = taskCount;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
