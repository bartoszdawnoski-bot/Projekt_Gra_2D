package com.walka.kibolgrad;

public class Flare extends ConsumableItem {
    float damage;
    public Flare(int count, float damage) {
        super("Raca", count, 600f);
        this.damage = damage;
    }

    @Override
    protected void applyEffect(Fighter user, Fighter target) {
        user.shootFlare(damage);
        System.out.println(user.getTeam() + " odpala race!");
    }
}
