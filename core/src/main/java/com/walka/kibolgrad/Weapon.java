package com.walka.kibolgrad;

public class Weapon {
    private String name;
    private float damageMultiplier;
    private float range;

    private float durationAdded;
    private float cost;

    public Weapon(String name, float damageMultiplier, float range, float weight) {
        this.name = name;
        this.damageMultiplier = damageMultiplier;
        this.range = range;

        this.durationAdded = weight * 0.1f;
        this.cost = weight * 0.5f;
    }

    public void equipTo(FighterStats stats) {
        stats.range = this.range;

        stats.strongAttackDamage *= this.damageMultiplier;
        stats.attackDamage *= this.damageMultiplier;

        stats.strongAttackDuration += this.durationAdded;
        stats.attackDuration += this.durationAdded;

        stats.attackCost += cost;
    }

    public float getDamageMultiplier() { return damageMultiplier; }
    public float getRange() { return range; }
    public String getName() { return name; }
    public float getDurationAdded() { return durationAdded; }
    public float getCost() { return cost; }
}
