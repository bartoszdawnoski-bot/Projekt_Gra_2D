package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public class AIInput implements FighterController{
    private Fighter self;
    private Array<Fighter> fighters;
    private Fighter target;
    private float scanTime = 0;
    private float behaviorTimer = 0;
    private int currentBehavior = 0;
    private float blockTimer = 0;
    private float reactionTimer = 0;
    private boolean playerAttackSpotted = false;

    public AIInput(Array<Fighter> fighters)
    {
        this.fighters = fighters;
    }

    public void setSelf(Fighter self)
    {
        this.self = self;
    }

    public  void closestTarget()
    {
        float minDistance = Float.MAX_VALUE;
        Fighter closets = null;

        for (Fighter f : fighters)
        {
            if(f.getTeam() != self.getTeam() && f.getFighterState() != FighterState.DEAD && f != self)
            {
                float dist = self.getPosition().dst(f.getPosition());
                if(dist < minDistance)
                {
                    minDistance = dist;
                    closets = f;
                }
            }
        }
        this.target = closets;
    }


    @Override
    public boolean wantsToAttack() {
        if(target == null || self.getFighterState() == FighterState.STAGGERED) return false;

        float distance = self.getPosition().dst(target.getPosition());

        if (self.getCurrentStamina() < 20) return false;

        return currentBehavior == 0 && distance <= self.getStats().range + 20f && Math.random() < 0.07;
    }

    @Override
    public boolean wantsToStrongAttack() {return false; }

    @Override
    public boolean wantsToDodge() {
        return false;
    }

    @Override
    public boolean wantsToBlock() {
        if (target == null) return false;

        if (blockTimer > 0) {
            blockTimer -= Gdx.graphics.getDeltaTime();
            return true;
        }

        boolean playerIsAttacking = (target.getFighterState() == FighterState.ATTACKING);
        if (playerIsAttacking && !playerAttackSpotted) {
            playerAttackSpotted = true;
            reactionTimer = 0.2f;
        }

        if (!playerIsAttacking) {
            playerAttackSpotted = false;
            reactionTimer = 0;
        }

        if (playerAttackSpotted) {
            reactionTimer -= Gdx.graphics.getDeltaTime();
            if (reactionTimer <= 0 && Math.random() < 0.6) {
                blockTimer = 0.5f;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean wantsToSprint() {
        return false;
    }

    @Override
    public Vector2 getMovementDirection() {
        scanTime += Gdx.graphics.getDeltaTime();
        behaviorTimer += Gdx.graphics.getDeltaTime();

        if(scanTime >= 0.5f || target == null || target.getFighterState() == FighterState.DEAD) {
            this.closestTarget();
            scanTime = 0;
        }

        Vector2 vector = new Vector2();
        if(target == null || self == null) return vector;
        float dist = self.getPosition().dst(target.getPosition());

        if(behaviorTimer > 1.2f) {
            float r = (float)Math.random();
            if(r < 0.7f) currentBehavior = 0;
            else if(r < 0.9f) currentBehavior = 1;
            else currentBehavior = 2;
            behaviorTimer = 0;
        }

        float time = self.getStateTimer();

        if (currentBehavior == 0) {
            float drift = (float)Math.sin(time * 2f) * 0.5f;
            vector.set(target.getPosition().x - self.getPosition().x,
                target.getPosition().y - self.getPosition().y + (drift * 60f));
        }
        else if (currentBehavior == 1) {
            vector.set(self.getPosition().x - target.getPosition().x,
                self.getPosition().y - target.getPosition().y);
        }
        else {
            float jitter = (float)Math.sin(time * 4f) > 0 ? 1 : -1;
            vector.set(jitter * 40f, (float)Math.cos(time * 2f) * 30f);
        }

        if (dist < 40f) {
            vector.add(self.getPosition().x - target.getPosition().x,
                self.getPosition().y - target.getPosition().y).scl(2f);
        }

        return vector.nor();
    }
}
