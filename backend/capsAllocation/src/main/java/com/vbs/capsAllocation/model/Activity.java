package com.vbs.capsAllocation.model;

public enum Activity {
    ABSENTEEISM(154, "Absenteeism"),
    TRAINING(156, "Training"),
    PRODUCTION(158, "Production"),
    MEETING(160, "Meeting"),
    EVENTS(162, "Events"),
    DOWNTIME(164, "Downtime"),
    COVID_SYSTEM_ISSUE(166, "Covid System Issue"),
    NO_VOLUME(168, "No Volume"),
    OTHER_PROJECTS(170, "Other Projects"),
    HOLIDAY(172, "Holiday"),
    COMPOFF(4292, "CompOff");

    private final int id;
    private final String name;

    Activity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Activity getById(int id) {
        for (Activity activity : values()) {
            if (activity.id == id) {    
                return activity;
            }
        }
        throw new IllegalArgumentException("No activity with id " + id);
    }
}
