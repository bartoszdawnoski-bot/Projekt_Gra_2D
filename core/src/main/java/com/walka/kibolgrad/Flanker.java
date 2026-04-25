package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Flanker implements AiLogicModule {
    private final Vector2 moveVector = new Vector2();
    private float dodgeTimer = 0f;
    private float intelligence = 0.6f;

    @Override
    public Vector2 getMovement(Fighter self, Fighter target, Vector2 targetPoint) {
        moveVector.setZero();
        if (target == null || target.getFighterState() == FighterState.DEAD) return moveVector;

        Vector2 toTarget = new Vector2(targetPoint).sub(self.getPosition());
        float distToPoint = toTarget.len();

        if (distToPoint > 15f) {
            float speedMult = 0.7f;

            Vector2 circleMove = new Vector2(-toTarget.y, toTarget.x).nor().scl(speedMult * 0.8f);

            moveVector.set(toTarget).nor().add(circleMove).nor().scl(speedMult);
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
        float attackThreshold = self.getStats().width + self.getStats().range + 10f;

        if (targetDistanceX <= attackThreshold && Math.abs(self.getPosition().y - target.getPosition().y) < self.getStats().range) {

            boolean playerFacingAway = (botIsToTheLeft && target.getFacingDirection().x == 1) || (!botIsToTheLeft && target.getFacingDirection().x == -1);
            boolean playerVulnerable = target.getFighterState() == FighterState.STAGGERED || target.getFighterState() == FighterState.ATTACKING;

            if (playerFacingAway || playerVulnerable) {
                return MathUtils.randomBoolean(0.9f * intelligence);
            } else {
                // ZWIĘKSZONO prawdopodobieństwo ataku od frontu
                return MathUtils.randomBoolean(0.25f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToStrongAttack(Fighter self, Fighter target) {
        if (target == null || target.getFighterState() == FighterState.DEAD) return false;

        float targetDistanceX = Math.abs(self.getPosition().x - target.getPosition().x);
        float attackThreshold = self.getStats().width + self.getStats().range + 10f;

        if (targetDistanceX <= attackThreshold && Math.abs(self.getPosition().y - target.getPosition().y) < self.getStats().range) {
            boolean botIsToTheLeft = self.getPosition().x < target.getPosition().x;
            boolean playerFacingAway = (botIsToTheLeft && target.getFacingDirection().x == 1) || (!botIsToTheLeft && target.getFacingDirection().x == -1);

            if (playerFacingAway) {
                return MathUtils.randomBoolean(0.7f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToBlock(Fighter self, Fighter target) {
        return false;
    }

    @Override
    public boolean wantsToDodge(Fighter self, Fighter target) {
        if (target == null) return false;

        if (dodgeTimer > 0f) {
            dodgeTimer -= Gdx.graphics.getDeltaTime();
            return false;
        }

        boolean enemyIsAttacking = (target.getFighterState() == FighterState.ATTACKING || target.getFighterState() == FighterState.STRONG_ATTACKING);
        float dist = self.getPosition().dst(target.getPosition());
        boolean enemyIsClose = dist < target.getStats().range + 40f;

        if (enemyIsAttacking && enemyIsClose) {
            if (Math.random() < 0.7f * intelligence) {
                dodgeTimer = 1.5f;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean wantsToSprint(Fighter self, Fighter target, Vector2 targetPoint) {
        float distToTarget = target.getPosition().dst(self.getPosition());
        return distToTarget > 200f && self.getCurrentStamina() > self.getStats().maxStamina * 0.6f;
    }
}
