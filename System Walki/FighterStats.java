package com.walka.kibolgrad;

public class FighterStats {
    //statystyki bazowe
    public float dexterity;
    public float strength;
    public float defense;
    public float weight;

    //statystyki pochodne
    public float maxHealth = 100f;
    public float maxStamina = 100f;
    public float speed = 250f;
    public float sprintSpeed = 450f;
    public float attackDuration = 0.6f;
    public float strongAttackDuration = 1.2f;
    public float attackDamage;
    public float strongAttackDamage;

    // Stałe fizyczne i wymiary
    public float acceleration = 2000f;
    public float sprintAcceleration = 3000f;
    public float friction = 0.85f;
    public float staminaRegen = 20f;
    public float range = 60f;
    public float height = 90f;
    public float width = 40f;

    //koszty akcji
    public float strongAttackCost = 35f;
    public float dodgeCost = 25f;
    public float sprintCost = 30f;
    public float attackCost = 15f;

    public FighterStats(float str, float dex, float def, float wgt) {
        this.strength = str;
        this.dexterity = dex;
        this.defense = def;
        this.weight = wgt;

        recalculate();
    }

    public void recalculate() {
        // SIŁA
        this.maxHealth = 50f + (strength * 10f);
        this.attackDamage = strength * 2f;
        this.strongAttackDamage = strength * 5f;

        // ZRĘCZNOŚĆ
        this.maxStamina = 100f + (dexterity * 5f);
        this.attackDuration = Math.max(0.2f, 0.6f - (dexterity * 0.015f));
        this.strongAttackDuration = Math.max(0.5f, 1.1f - (dexterity * 0.02f));

        // WAGA
        this.speed = Math.max(100f, 300f + (dexterity * 5f) - (weight * 2f)) * 0.5f;
        this.sprintSpeed = this.speed * 1.6f;
        this.acceleration = Math.max(800f, 3000f - (weight * 40f));
        this.sprintAcceleration = this.acceleration * 1.5f;
        this.dodgeCost = 15f + (weight * 0.4f) - (dexterity * 0.2f);
        this.staminaRegen = Math.max(5f, 25f - (weight * 0.3f) + (dexterity * 0.2f)) * 4;

        // OBRONA
        this.dodgeCost = Math.max(10f, 35f - (dexterity * 0.5f));

    }
}
