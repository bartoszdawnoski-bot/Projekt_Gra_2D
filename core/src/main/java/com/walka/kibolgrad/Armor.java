package com.walka.kibolgrad;

public class Armor {
    private String name;
    private float armorPoints;

    public Armor(String name, float armorPoints) {
        this.name = name;
        this.armorPoints = armorPoints;
    }

    public String getName() { return name; }
    public float getArmorPoints() { return armorPoints; }
}
