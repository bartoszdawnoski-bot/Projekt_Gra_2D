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
    private final float AVOID_RADIUS = 90f;
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

    @Override
    public boolean wantsToAttack() {
        if (target == null || target.getFighterState() == FighterState.DEAD || !hasAttackToken) return false;
        if (self.getFighterState() == FighterState.STAGGERED) return false;

        float distX = Math.abs(self.getPosition().x - target.getPosition().x);
        float distY = Math.abs(self.getPosition().y - target.getPosition().y);

        if (distX < (self.getStats().range + 30f) && distY < 20f) {
            return MathUtils.randomBoolean(0.6f * intelligence);
        }
        return false;
    }

    @Override
    public boolean wantsToStrongAttack() {
        if(target == null || !hasAttackToken || target.getFighterState() == FighterState.DEAD) return false;
        if(self.getFighterState() == FighterState.STAGGERED) return false;

        float distX = Math.abs(self.getPosition().x - target.getPosition().x);
        float distY = Math.abs(self.getPosition().y - target.getPosition().y);

        if (distX < (self.getStats().range + 30f) && distY < 20f) {
            FighterState enemyState = target.getFighterState();

            if(enemyState == FighterState.STAGGERED || enemyState == FighterState.BLOCKING) return MathUtils.randomBoolean(0.6f * intelligence);
            return MathUtils.randomBoolean(0.1f * intelligence);
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

        boolean enemyIsAttacik = (target.getFighterState() == FighterState.ATTACKING || target.getFighterState() == FighterState.DEAD);
        float dist = self.getPosition().dst(target.getPosition());
        boolean enemyIsClose = dist < target.getStats().range + 40f;

        if(enemyIsAttacik && enemyIsClose) {
            if(Math.random() < 0.3f * intelligence) {
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
                if (Math.random() < 0.70f * intelligence) {
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
        if (target == null || self.getFighterState() == FighterState.DEAD) return false;
        float distToPoint = targetPoint.dst(self.getPosition());

        if(distToPoint > 300f && self.getCurrentStamina() > self.getStats().maxStamina * 0.5f) {
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

        // Blokada ruchu podczas akcji
        FighterState state = self.getFighterState();
        if (state == FighterState.ATTACKING || state == FighterState.STRONG_ATTACKING ||
            state == FighterState.STAGGERED || state == FighterState.BLOCKING) {
            return movementVector;
        }

        // Dystans i wektor do celu
        tempToTarget.set(targetPoint).sub(self.getPosition());
        float distToPoint = tempToTarget.len();

        if (distToPoint < 20f) {
            return movementVector.setZero();
        }

        // --- SPALENIE SZYBKOŚCI ---
        // Boty są teraz znacznie wolniejsze. Agresor (0.7f), reszta (0.4f).
        float speedMult = hasAttackToken ? 0.7f : 0.4f;

        if (distToPoint < 120f) {
            speedMult *= (distToPoint / 120f);
        }

        // Agresywne wyrównanie osi Y z daleka
        if (distToPoint > 15f) {
            baseMove.set(tempToTarget.x, tempToTarget.y * 5f).nor().scl(speedMult);
        } else {
            baseMove.set(tempToTarget).nor().scl(speedMult);
        }

        separation.setZero();
        int obstacles = 0;
        boolean criticalOverlap = false;

        // Separacja i ślizg
        for (int i = 0; i < fighters.size; i++) {
            Fighter f = fighters.get(i);

            if (f != self && f != target && f.getFighterState() != FighterState.DEAD) {
                float d = self.getPosition().dst(f.getPosition());

                // Zwiększony promień unikania (z 90 na 100), żeby się nie kleili
                if (d < 100f && d > 1) {
                    obstacles++;
                    tempPush.set(self.getPosition()).sub(f.getPosition()).nor();
                    float weight = (100f - d) / 100f;

                    // Krytyczne nałożenie (stoją na sobie)
                    if (d < 50f) criticalOverlap = true;

                    // Mocniejsze odpychanie
                    separation.add(tempPush.x * weight * 6.0f, tempPush.y * weight * 6.0f);

                    // Inteligentny ślizg
                    float slideX = 0;
                    float slideY = 0;
                    if (Math.abs(tempPush.y) > Math.abs(tempPush.x)) {
                        if (Math.abs(self.getPosition().x - f.getPosition().x) > 1.5f)
                            slideX = (self.getPosition().x > f.getPosition().x) ? 1f : -1f;
                        else slideX = (self.hashCode() > f.hashCode()) ? 1f : -1f;
                    } else {
                        if (Math.abs(self.getPosition().y - f.getPosition().y) > 1.5f)
                            slideY = (self.getPosition().y > f.getPosition().y) ? 1f : -1f;
                        else slideY = (self.hashCode() > f.hashCode()) ? 1f : -1f;
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

        // --- ŁĄCZENIE SIŁ (REBALANS) ---
        if (criticalOverlap) {
            // Jeśli stoją na sobie, ruch do celu jest ZEROWANY. Tylko ucieczka na boki!
            movementVector.set(separation).nor().scl(speedMult);
        } else if (obstacles > 0) {
            // Ruch do celu ma tylko 5% wpływu (0.05), separacja ma 95% (0.95).
            // To rozbije każdą grupę, bo boty przestaną się pchać na gracza przez plecy kolegów.
            movementVector.set(baseMove).scl(0.05f).add(separation.scl(0.95f)).nor().scl(speedMult);
        } else {
            movementVector.set(baseMove);
        }

        // Wyciąganie ze stłuczki
        if (obstacles > 0 && movementVector.len() < 0.2f && distToPoint > 20f) {
            escapeToCenter.set(900f - self.getPosition().x, 500f - self.getPosition().y).nor();
            movementVector.set(escapeToCenter).scl(speedMult);
        }

        return movementVector;
    }
}
