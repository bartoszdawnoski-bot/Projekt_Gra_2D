package com.walka.kibolgrad;

public abstract class ConsumableItem {
    protected String name;
    protected int count;
    protected float useRange;

    public ConsumableItem(String name, int count, float useRange) {
        this.name = name;
        this.count = count;
        this.useRange = useRange;
    }

    public boolean tryUse(Fighter user, Fighter target) {
        if (count <= 0) {
            System.out.println("Brak przedmiotu: " + name);
            return false;
        }

        Fighter actualTarget = (target != null) ? target : user;

        if (user != actualTarget) {
            float distance = user.getPosition().dst(actualTarget.getPosition());

            if (distance > useRange) {
                System.out.println(" Podejdź bliżej (Dystans: " + distance + ", Wymagane: " + useRange + ")");
                return false;
            }
        }

        applyEffect(user, actualTarget);
        count--;
        return true;
    }

    protected abstract void applyEffect(Fighter user, Fighter target);

    public String getName() { return name; }
    public int getCount() { return count; }
}
