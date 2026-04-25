package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Aggressor implements AiLogicModule {
    private final Vector2 moveVector = new Vector2();
    private float intelligence = 0.6f;
    private float blockTimer = 0f;

    @Override
    public Vector2 getMovement(Fighter self, Fighter target, Vector2 targetPoint) {
        moveVector.setZero();
        if (target == null || target.getFighterState() == FighterState.DEAD) return moveVector;

        float distToPoint = targetPoint.dst(self.getPosition());
        if (distToPoint > 35f) {
            Vector2 toTarget = new Vector2(targetPoint).sub(self.getPosition());
            moveVector.set(toTarget.x, toTarget.y * 5f).nor().scl(1.0f);
        }
        return moveVector;
    }

    @Override
    public boolean wantsToAttack(Fighter self, Fighter target) {
        if (target == null || target.getFighterState() == FighterState.DEAD) return false;
        if (self.getFighterState() == FighterState.STAGGERED) return false;

        float facingX = self.getFacingDirection().x;
        boolean botIsToTheLeft = self.getPosition().x < target.getPosition().x;
        if ((botIsToTheLeft && facingX != 1) || (!botIsToTheLeft && facingX != -1)) return false;

        float targetDistanceX = Math.abs(self.getPosition().x - target.getPosition().x);
        float attackThreshold = self.getStats().width + self.getStats().range;

        if (targetDistanceX <= attackThreshold) {
            float distY = Math.abs(self.getPosition().y - target.getPosition().y);
            if (distY < self.getStats().range) {
                return MathUtils.randomBoolean(0.8f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToStrongAttack(Fighter self, Fighter target) {
        if (target == null || target.getFighterState() == FighterState.DEAD) return false;

        float targetDistanceX = Math.abs(self.getPosition().x - target.getPosition().x);
        float attackThreshold = self.getStats().width + self.getStats().range;

        if (targetDistanceX <= attackThreshold && Math.abs(self.getPosition().y - target.getPosition().y) < self.getStats().range) {
            if (target.getFighterState() == FighterState.BLOCKING) {
                return MathUtils.randomBoolean(0.8f * intelligence);
            }
            return MathUtils.randomBoolean(0.2f * intelligence);
        }
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
        boolean enemyIsClose = dist < target.getStats().range + 30f;

        if (enemyIsAttacking && enemyIsClose) {
            if (Math.random() < 0.4f * intelligence) {
                blockTimer = 0.5f;
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
        if (target == null || target.getFighterState() == FighterState.DEAD) return false;

        float distToTarget = target.getPosition().dst(self.getPosition());
        return distToTarget > 150f && self.getCurrentStamina() > self.getStats().maxStamina * 0.3f;
    }
}
