package com.walka.kibolgrad;

public class MedKit extends ConsumableItem {
    private float healAmount;

    public MedKit(int count, float healAmount) {
        super("Apteczka", count, 150f);
        this.healAmount = healAmount;
    }

    @Override
    protected void applyEffect(Fighter user, Fighter target) {
        target.heal(healAmount);

        if (user == target) {
            System.out.println("Uleczyles siebie o " + healAmount + " HP");
        } else {
            System.out.println("Uleczyles sojusznika o " + healAmount + " HP");
        }
    }
}
