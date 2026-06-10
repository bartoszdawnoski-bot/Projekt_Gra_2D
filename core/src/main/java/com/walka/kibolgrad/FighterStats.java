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
    public float damageReduction;
    public float knockbackPower;

    // Stałe fizyczne i wymiary
    public float acceleration = 2000f;
    public float sprintAcceleration = 3000f;
    public float friction = 0.85f;
    public float staminaRegen = 20f;
    public float range = 30f;
    public float height = 80f;
    public float width = 35f;

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
        this.maxHealth = 50f + (strength * 10f) + (weight * 2f);

        this.attackDamage = 10f + (strength * 1.5f);
        this.strongAttackDamage = 25f + (strength * 3.5f);

        this.knockbackPower = 600f + (strength * 15f) + (weight * 10f);

        //ZRĘCZNOŚĆ
        this.maxStamina = 100f + (dexterity * 5f);
        this.attackDuration = 0.6f / (1f + (dexterity * 0.05f));
        this.strongAttackDuration = 1.2f / (1f + (dexterity * 0.04f));

        //OBRONA
        this.damageReduction = Math.min(0.8f, defense * 0.015f);

        //WAGA I FIZYKA
        this.speed = Math.max(50f, 150f + (dexterity * 5f) - (weight * 2f));
        this.sprintSpeed = this.speed * 1.6f;

        this.acceleration = Math.max(800f, 1500f - (weight * 40f));
        this.sprintAcceleration = this.acceleration * 1.5f;

        this.dodgeCost = Math.max(10f, 15f + (weight * 0.4f) - (dexterity * 0.2f));
        this.staminaRegen = Math.max(5f, 25f - (weight * 0.3f) + (dexterity * 0.8f));

        this.attackCost = 15f;
        this.strongAttackCost = 35f;
    }
}
