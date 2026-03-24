package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class AIInput implements FighterController {
    //REFERENCJE
    private Fighter self;
    private Array<Fighter> fighters;
    private Fighter target;

    //STAN BOTA
    private Role currentRole = Role.WATCHER;
    private boolean hasAttackToken = false;
    private float blockTimer = 0;
    private boolean playerAttackSpotted = false;
    private float dodgeTimer = 0f;
    private float intelligence = 1f;

    //STAŁE PARAMETRY AI
    private final float MARGIN = 100f;
    private final float ESCAPE_FORCE = 5.0f;
    private final float SLIDE_FORCE = 5.0f;

    //WEKTORY GLOBALNE
    private final Vector2 targetPoint = new Vector2();
    private final Vector2 movementVector = new Vector2();
    private final Vector2 tempToTarget = new Vector2();
    private final Vector2 tempPush = new Vector2();
    private final Vector2 baseMove = new Vector2();
    private final Vector2 separation = new Vector2();
    private final Vector2 escapeToCenter = new Vector2();

    public AIInput(Array<Fighter> fighters) {
        this.fighters = fighters;
    }

    public void setSelf(Fighter self) {
        this.self = self;
    }

    public void setOrder(Fighter target, Role currentRole, Vector2 targetPoint, boolean hasAttackToken) {
        this.target = target;
        this.currentRole = currentRole;
        this.targetPoint.set(targetPoint);
        this.hasAttackToken = hasAttackToken;
    }

    public Fighter getTarget() {
        return target;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    @Override
    public boolean wantsToAttack() {
        if (target == null || target.getFighterState() == FighterState.DEAD || !hasAttackToken) return false;
        if (self.getFighterState() == FighterState.STAGGERED) return false;

        float currentX = self.getPosition().x;
        float targetX = target.getPosition().x;
        float facingX = self.getFacingDirection().x;
        boolean botIsToTheLeft = currentX < targetX;

        boolean facingRightDirection = (botIsToTheLeft && facingX == 1) || (!botIsToTheLeft && facingX == -1);
        if (!facingRightDirection) return false;

        float targetDistanceX = Math.abs(currentX - targetX);

        if (targetDistanceX <= (self.getStats().range + 10f)) {
            float distY = Math.abs(self.getPosition().y - target.getPosition().y);
            if (distY < self.getStats().range) {
                return MathUtils.randomBoolean(0.6f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToStrongAttack() {
        if(target == null || !hasAttackToken || target.getFighterState() == FighterState.DEAD) return false;
        if(self.getFighterState() == FighterState.STAGGERED) return false;

        float currentX = self.getPosition().x;
        float targetX = target.getPosition().x;
        float facingX = self.getFacingDirection().x;
        boolean botIsToTheLeft = currentX < targetX;
        boolean facingRightDirection = (botIsToTheLeft && facingX == 1) || (!botIsToTheLeft && facingX == -1);
        if (!facingRightDirection) return false;

        float targetDistanceX = Math.abs(currentX - targetX);
        if (targetDistanceX <= (self.getStats().range + 10f)) {
            float distY = Math.abs(self.getPosition().y - target.getPosition().y);
            if (distY < self.getStats().range) {
                FighterState enemyState = target.getFighterState();
                if (enemyState == FighterState.STAGGERED || enemyState == FighterState.BLOCKING)
                    return MathUtils.randomBoolean(0.6f * intelligence);
                return MathUtils.randomBoolean(0.1f * intelligence);
            }
        }
        return false;
    }

    @Override
    public boolean wantsToDodge() {
        if(target == null) return false;
        if(dodgeTimer > 0f)
        {
            dodgeTimer -= Gdx.graphics.getDeltaTime();
            return false;
        }

        boolean enemyIsAttacik = (target.getFighterState() == FighterState.ATTACKING && target.getFighterState() != FighterState.DEAD);
        float dist = self.getPosition().dst(target.getPosition());
        boolean enemyIsClose = (dist < target.getStats().range + 40f);

        float roleModifier = 1.0f;
        if (currentRole == Role.STALLER) roleModifier = 1.5f;
        else if (currentRole == Role.AGRESSOR) roleModifier = 0.6f;

        if(enemyIsAttacik && enemyIsClose) {
            if(Math.random() < 0.3f * intelligence * roleModifier) {
                dodgeTimer = 2.0f;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean wantsToBlock() {
        if (target == null) return false;

        if (blockTimer > 0f) {
            blockTimer -= Gdx.graphics.getDeltaTime();
            return true;
        }
        boolean enemyIsAttacking = (target.getFighterState() == FighterState.ATTACKING || target.getFighterState() == FighterState.STRONG_ATTACKING);
        float dist = self.getPosition().dst(target.getPosition());
        boolean enemyIsClose = dist < target.getStats().range + 20f;

        if (enemyIsAttacking && enemyIsClose) {
            if (!playerAttackSpotted) {
                playerAttackSpotted = true;

                float roleModifier = 1.0f;
                if (currentRole == Role.STALLER) roleModifier = 1.5f;
                else if (currentRole == Role.AGRESSOR) roleModifier = 0.5f;

                if (Math.random() < 0.70f * intelligence * roleModifier) {
                    blockTimer = 0.5f;
                    return true;
                }
            }
        } else {
            playerAttackSpotted = false;
        }
        return false;
    }

    @Override
    public boolean wantsToSprint() {
        if (currentRole == Role.WATCHER) return false;
        if (target == null || self.getFighterState() == FighterState.DEAD) return false;
        float distToPoint = targetPoint.dst(self.getPosition());
        float distToBack = targetPoint.dst(self.getPosition());

        if(currentRole == Role.FLANKER && distToBack > 200f && self.getCurrentStamina() > self.getStats().maxStamina * 0.4f) {
            return true;
        }

        if((distToPoint > 300f && self.getCurrentStamina() > self.getStats().maxStamina * 0.4f) || self.getStats().maxHealth <= self.getStats().maxHealth * 0.4f){
            return true;
        }
        return false;
    }

    @Override
    public Vector2 getMovementDirection() {
        movementVector.setZero();

        if (target == null || self == null || target.getFighterState() == FighterState.DEAD) {
            return movementVector;
        }

        // Obrót do celu
        self.getFacingDirection().x = (target.getPosition().x > self.getPosition().x) ? 1 : -1;

        // Wektor od bota do przypisanego punktu
        tempToTarget.set(targetPoint).sub(self.getPosition());
        float distToPoint = tempToTarget.len();

        if (distToPoint < 20f) {
            return movementVector.setZero();
       }

        // LOGIKA ZALEŻNA OD ROLI
        float speedMult = 0.4f;
        float targetWeight = 0.1f;
        baseMove.setZero();

        switch (currentRole) {
            case AGRESSOR:
                speedMult = 1f;
                targetWeight = 0.6f;

                if (distToPoint > 15f) {
                    baseMove.set(tempToTarget.x, tempToTarget.y * 5f).nor().scl(speedMult);
                } else {
                    baseMove.set(tempToTarget).nor().scl(speedMult);
                }
                break;

            case FLANKER:
                speedMult = 0.6f;
                targetWeight = 0.3f;

                Vector2 toTarget = new Vector2(self.getPosition()).sub(target.getPosition());
                Vector2 circleMove = new Vector2(-toTarget.y, toTarget.x).nor().scl(speedMult * 0.5f);

                baseMove.set(tempToTarget).nor().add(circleMove).nor().scl(speedMult);
                break;

            case STALLER:
                speedMult = 0.9f;
                targetWeight = 0.4f;
                baseMove.set(tempToTarget).nor().scl(speedMult);
                break;

            case WATCHER:
                speedMult = 0.3f;
                targetWeight = 0.05f;
                baseMove.set(tempToTarget).nor().scl(speedMult);
                break;
        }

        if (distToPoint < 120f) {
            baseMove.scl(distToPoint / 120f);
        }

        // SYSTEM SEPARACJI
        separation.setZero();
        int obstacles = 0;
        boolean criticalOverlap = false;
        float dRange = 100f;
        float sMultipler = 6f;

        for (int i = 0; i < fighters.size; i++) {
            Fighter f = fighters.get(i);

            if (f != self && f != target && f.getFighterState() != FighterState.DEAD) {
                float d = self.getPosition().dst(f.getPosition());

                if (d < dRange && d > 1f) {
                    obstacles++;
                    tempPush.set(self.getPosition()).sub(f.getPosition()).nor();
                    float weight = (dRange - d) / dRange;

                    if (d < 30f) criticalOverlap = true;

                    separation.add(tempPush.x * weight * sMultipler, tempPush.y * weight * sMultipler);

                    float slideX = 0;
                    float slideY = 0;
                    if (Math.abs(tempPush.y) > Math.abs(tempPush.x)) {
                        slideX = (self.getPosition().x > f.getPosition().x) ? 1f : -1f;
                    } else {
                        slideY = (self.getPosition().y > f.getPosition().y) ? 1f : -1f;
                    }
                    separation.add(slideX * weight * SLIDE_FORCE, slideY * weight * SLIDE_FORCE);
                }
            }
        }

        // Odpychanie od krawędzi mapy
        if (self.getPosition().x < MARGIN) { separation.add(ESCAPE_FORCE, 0); obstacles++; }
        else if (self.getPosition().x > self.getScreenWidthEnd() - 50f - MARGIN) { separation.add(-ESCAPE_FORCE, 0); obstacles++; }
        if (self.getPosition().y < MARGIN) { separation.add(0, ESCAPE_FORCE); obstacles++; }
        else if (self.getPosition().y > self.getScreenHeightEnd() - 50f - MARGIN) { separation.add(0, -ESCAPE_FORCE); obstacles++; }

        //APLIKACJA WAG
        if (criticalOverlap) {
            movementVector.set(separation).nor().scl(speedMult);
        } else if (obstacles > 0) {
            float sepWeight = 1.0f - targetWeight;
            movementVector.set(baseMove).scl(targetWeight).add(separation.scl(sepWeight)).nor().scl(speedMult);
        } else {
            movementVector.set(baseMove);
        }

        // Wyciąganie
        if (obstacles > 0 && movementVector.len() < 0.2f && distToPoint > 20f) {
            escapeToCenter.set(900f - self.getPosition().x, 500f - self.getPosition().y).nor();
            movementVector.set(escapeToCenter).scl(speedMult);
        }

        return movementVector;

    }
}
