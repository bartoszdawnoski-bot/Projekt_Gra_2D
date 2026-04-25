package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Watcher implements AiLogicModule {
    private final Vector2 moveVector = new Vector2();
    private float blockTimer = 0f;
    private float intelligence = 0.6f;

    @Override
    public Vector2 getMovement(Fighter self, Fighter target, Vector2 targetPoint) {
        moveVector.setZero();
        if (target == null || target.getFighterState() == FighterState.DEAD) return moveVector;

        float distToPoint = targetPoint.dst(self.getPosition());
        if (distToPoint > 10f) {
            Vector2 toTarget = new Vector2(targetPoint).sub(self.getPosition());
            moveVector.set(toTarget).nor().scl(0.5f);
        }
        return moveVector;
    }

    @Override
    public boolean wantsToAttack(Fighter self, Fighter target) {
        if (target == null || target.getFighterState() == FighterState.DEAD) return false;

        float dist = self.getPosition().dst(target.getPosition());
        if (dist < self.getStats().range + 10f) {
            if (target.getFighterState() == FighterState.STAGGERED) {
                return MathUtils.randomBoolean(0.3f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToStrongAttack(Fighter self, Fighter target) {
        return false;
    }

    @Override
    public boolean wantsToBlock(Fighter self, Fighter target) {
        if (target == null) return false;

        if (blockTimer > 0f) {
            blockTimer -= Gdx.graphics.getDeltaTime();
            return true;
        }

        boolean enemyIsAttacking = (target.getFighterState() == FighterState.ATTACKING || target.getFighterState() == FighterState.STRONG_ATTACKING);
        float dist = self.getPosition().dst(target.getPosition());
        boolean enemyIsClose = dist < target.getStats().range + 50f;

        if (enemyIsAttacking && enemyIsClose) {
            if (Math.random() < 0.8f * intelligence) {
                blockTimer = 0.8f;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean wantsToDodge(Fighter self, Fighter target) {
        return false;
    }

    @Override
    public boolean wantsToSprint(Fighter self, Fighter target, Vector2 targetPoint) {
        return false;
    }
}
