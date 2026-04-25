package com.walka.kibolgrad;

public class Speed extends ConsumableItem {
    private float duration;

    public Speed(int count ,float duration) {
        super("Amfetamina",count, 150f);
        this.duration = duration;
    }

    @Override
    protected void applyEffect(Fighter user, Fighter target){
        target.applyAmphetamineBuff(duration);

        if (user == target) {
            System.out.println("Nieskonczona stamina przez " + duration + "s");
        } else {
            System.out.println("Podałes Amfetamine sojusznikowi");
        }
    }

}
