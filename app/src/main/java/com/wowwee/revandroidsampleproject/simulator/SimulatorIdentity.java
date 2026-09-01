package com.wowwee.revandroidsampleproject.simulator;

public class SimulatorIdentity {
    private final String id;
    private final String name;
    private final String colorHex;

    public SimulatorIdentity(String id, String name, String colorHex) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColorHex() {
        return colorHex;
    }
}

