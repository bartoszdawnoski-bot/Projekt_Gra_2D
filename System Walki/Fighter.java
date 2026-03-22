package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Fighter {
    // REFERENCJE
    private FighterStats stats;
    private Team team;
    private FighterController controller;

    //STAN I ZEGARY
    private FighterState state = FighterState.IDLE;
    private float stateTimer = 0f;
    private float hitCooldown = 0f;
    private final float INVULNERABILITY_TIME = 0.5f;

    // STATYSTYKI BIEŻĄCE
    private float currentHealth;
    private float currentStamina;
    private boolean isBlocking = false;
    private boolean hasHitTarget = false;
    private float knockback = 1000f;

    //POZYCJA I WEKTORY
    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Vector2 facingDirection = new Vector2(1, 0);

    //HITBOXY
    private final Rectangle hurtBox = new Rectangle();
    private final Rectangle hitBox = new Rectangle();
    private float hitWidth = 30f;
    private float hitHeight = 50f;

    // GRANICE MAPY
    private float screenWidthStart = 0f;
    private float screenWidthEnd = (float) Gdx.graphics.getWidth() - 100; ;
    private float screenHeightStart = 0f;
    private float screenHeightEnd = (float) Gdx.graphics.getHeight() - 100;

    public Fighter(Vector2 position, FighterController controller, FighterStats stats, Team team) {
        this.position.set(position);
        this.controller = controller;
        this.stats = stats;
        this.team = team;

        currentHealth = stats.maxHealth;
        currentStamina = stats.maxStamina;

        hurtBox.set(position.x, position.y, stats.width, stats.height);
        hitBox.set(position.x, position.y, 0f, 0f);
    }

    public void update(float delta) {
        stateTimer += delta;

        if (hitCooldown > 0) {
            hitCooldown -= delta;
        }

        // OBSŁUGA STANÓW
        switch (state) {
            case IDLE:
            case MOVING: {
                Vector2 moveInput = controller.getMovementDirection();

                if (controller.wantsToDodge() && currentStamina > stats.dodgeCost) {
                    state = FighterState.DODGING;
                    currentStamina -= stats.dodgeCost;
                    stateTimer = 0f;

                    if (!moveInput.isZero()) {
                        velocity.set(moveInput).nor().scl(stats.speed * 1.2f);
                    } else {
                        velocity.set(-facingDirection.x * stats.speed * 0.8f, 0);
                    }
                } else if (currentStamina > stats.attackCost && controller.wantsToAttack()) {
                    state = FighterState.ATTACKING;
                    currentStamina -= stats.attackCost;
                    stateTimer = 0f;
                    hasHitTarget = false;
                } else if (currentStamina > stats.strongAttackCost && controller.wantsToStrongAttack()) {
                    state = FighterState.STRONG_ATTACKING;
                    currentStamina -= stats.strongAttackCost;
                    stateTimer = 0f;
                    hasHitTarget = false;
                } else if (currentStamina > 1f && controller.wantsToSprint()) {
                    state = FighterState.SPRINTING;
                    stateTimer = 0f;
                } else if (controller.wantsToBlock() && currentStamina >= 30f) {
                    state = FighterState.BLOCKING;
                    stateTimer = 0f;
                } else {
                    if (!moveInput.isZero()) {
                        state = FighterState.MOVING;
                        velocity.add(moveInput.x * stats.acceleration * delta, moveInput.y * stats.acceleration * delta);
                        velocity.limit(stats.speed);
                        if (moveInput.x != 0) {
                            facingDirection.set(moveInput.x > 0 ? 1 : -1, 0);
                        }
                    } else {
                        state = FighterState.IDLE;
                        velocity.scl(stats.friction);
                        if (velocity.len() < 5f) {
                            velocity.setZero();
                        }
                    }
                }

                // Regeneracja staminy
                if (state == FighterState.IDLE || state == FighterState.MOVING) {
                    if (currentStamina < stats.maxStamina) {
                        currentStamina += stats.staminaRegen * delta;
                    }
                }
                break;
            }
            case ATTACKING: {
                velocity.scl(stats.friction);

                if (stateTimer > stats.attackDuration * 0.2f && stateTimer < stats.attackDuration * 0.8f) {
                    float hitX = position.x + (facingDirection.x * stats.range);
                    if (facingDirection.x == -1) {
                        hitX = position.x + (facingDirection.x * stats.range) + hitWidth / 1.5f;
                    }

                    float hitY = position.y + hitHeight / 2;
                    hitBox.set(hitX, hitY, hitWidth, hitHeight);
                } else {
                    hitBox.set(0, 0, 0, 0);
                }

                if (stateTimer > stats.attackDuration) {
                    state = FighterState.IDLE;
                }
                break;
            }
            case STRONG_ATTACKING: {
                velocity.scl(stats.friction);

                if (stateTimer > stats.strongAttackDuration * 0.2f && stateTimer < stats.strongAttackDuration * 0.8f) {
                    float hitX = position.x + (facingDirection.x * stats.range);
                    if (facingDirection.x == -1) {
                        hitX = position.x + (facingDirection.x * stats.range) + hitWidth / 1.5f;
                    }

                    float hitY = position.y + hitHeight / 2;
                    hitBox.set(hitX, hitY, hitWidth, hitHeight);
                } else {
                    hitBox.set(0, 0, 0, 0);
                }

                if (stateTimer > stats.strongAttackDuration) {
                    state = FighterState.IDLE;
                }
                break;
            }
            case DODGING: {
                velocity.scl(0.98f);
                if (stateTimer < 0.2f) hurtBox.set(0, 0, 0, 0);
                else hurtBox.set(position.x, position.y, stats.width, stats.height);

                if (stateTimer > 0.3f) {
                    state = FighterState.IDLE;
                    velocity.setZero();
                }
                break;
            }
            case SPRINTING: {
                Vector2 moveInput = controller.getMovementDirection();
                if (currentStamina <= 0 || !controller.wantsToSprint() || moveInput.isZero()) {
                    state = FighterState.IDLE;
                } else {
                    currentStamina -= stats.sprintCost * delta;
                    velocity.add(moveInput.x * stats.sprintAcceleration * delta, moveInput.y * stats.sprintAcceleration * delta);
                    velocity.limit(stats.sprintSpeed);
                    facingDirection.set(moveInput).nor();
                }
                break;
            }
            case BLOCKING: {
                Vector2 moveInput = controller.getMovementDirection();
                if (!controller.wantsToBlock() || currentStamina <= 0) {
                    state = FighterState.IDLE;
                    isBlocking = false;
                } else {
                    isBlocking = true;
                    currentStamina -= 5f * delta;

                    if (!moveInput.isZero()) {
                        velocity.add(moveInput.x * stats.acceleration * 0.5f * delta, moveInput.y * stats.acceleration * 0.5f * delta);
                        velocity.limit(stats.speed * 0.3f);
                        facingDirection.set(moveInput).nor();
                        if (moveInput.x != 0) facingDirection.set(moveInput.x > 0 ? 1 : -1, 0);
                    } else {
                        velocity.scl(stats.friction);
                        if (velocity.len() < 5f) {
                            velocity.setZero();
                        }
                    }
                }
                break;
            }
            case STAGGERED: {
                velocity.scl(0.9f);
                hitBox.set(0, 0, 0, 0);

                if (stateTimer > 0.8f) {
                    state = FighterState.IDLE;
                }
                break;
            }
            case DEAD: {
                velocity.setZero();
                hitBox.set(0, 0, 0, 0);
                hurtBox.set(0, 0, 0, 0);
                break;
            }
        }

        // AKTUALIZACJA POZYCJI I KRAWDĘDZIE MAPY
        position.add(velocity.x * delta, velocity.y * delta);

        if (position.x < screenWidthStart) {
            position.x = screenWidthStart;
            velocity.x = 0;
        }

        if (position.y < screenHeightStart) {
            position.y = screenHeightStart;
            velocity.y = 0;
        }

        if (position.x > screenWidthEnd - stats.width) {
            position.x = screenWidthEnd - stats.width;
            velocity.x = 0;
        }

        if (position.y > screenHeightEnd - stats.height) {
            position.y = screenHeightEnd - stats.height;
            velocity.y = 0;
        }

        hurtBox.setPosition(position.x, position.y);
    }

    public void takeDamage(float damage, float staminaDamage, float attackerFacingX, boolean isStrongDamage) {
        if (state == FighterState.DODGING || hitCooldown > 0 || state == FighterState.DEAD) return;

        if (state == FighterState.BLOCKING && currentStamina > 0) {
            if (attackerFacingX != facingDirection.x) currentStamina -= staminaDamage * 1.5f;
            else currentStamina -= staminaDamage;

            velocity.x = attackerFacingX * 100f;

            float actualStaminaDamage = isStrongDamage ? staminaDamage * 3 : staminaDamage;
            currentStamina -= actualStaminaDamage;

            if (currentStamina <= 0) {
                currentStamina = 0;
                state = FighterState.STAGGERED;
                stateTimer = 0f;

                currentHealth -= damage * 0.2f;
                velocity.x = attackerFacingX * knockback;
                velocity.y = 0;
                hitCooldown = INVULNERABILITY_TIME;
            }
        } else {
            currentHealth -= damage;
            if (currentHealth < 0) currentHealth = 0;
            state = FighterState.STAGGERED;
            stateTimer = 0f;
            velocity.x = attackerFacingX * knockback;
            velocity.y = 0;
            hitCooldown = INVULNERABILITY_TIME;
            if (currentHealth <= 0) {
                state = FighterState.DEAD;
            }
        }
    }

    // RENDEROWANIE
    public void drawDebug(ShapeRenderer shape) {
        if (state == FighterState.ATTACKING) shape.setColor(Color.RED);
        else if (hitCooldown > 0) shape.setColor(Color.FIREBRICK);
        else if (state == FighterState.STRONG_ATTACKING) shape.setColor(Color.PINK);
        else if (state == FighterState.DODGING) shape.setColor(Color.GREEN);
        else if (state == FighterState.STAGGERED) shape.setColor(Color.BLUE);
        else if (state == FighterState.DEAD) shape.setColor(Color.GRAY);
        else if (state == FighterState.BLOCKING) shape.setColor(Color.ORANGE);
        else shape.setColor(Color.WHITE);

        shape.rect(hurtBox.x, hurtBox.y, hurtBox.width, hurtBox.height);

        if (hitBox.width > 0) {
            shape.setColor(Color.YELLOW);
            shape.rect(hitBox.x, hitBox.y, hitBox.width, hitBox.height);
        }
    }

    public void drawStatusBars(ShapeRenderer shape) {
        if (state == FighterState.DEAD) return;

        float barWidth = 50f;
        float barHeight = 5f;
        float spacing = 5f;
        float singHeight = 10f;
        float singWidth = 10f;

        float startX = position.x;
        float startY = position.y + 110f;

        // Pasek HP
        shape.setColor(Color.FIREBRICK);
        shape.rect(startX, startY, barWidth, barHeight);

        float healthPercent = currentHealth / stats.maxHealth;
        shape.setColor(Color.GREEN);
        shape.rect(startX, startY, barWidth * healthPercent, barHeight);

        // Pasek Staminy
        if (currentStamina < stats.maxStamina) {
            float staminaStartY = startY + barHeight + spacing;

            shape.setColor(Color.GRAY);
            shape.rect(startX, staminaStartY, barWidth, barHeight);

            float staminaPercent = currentStamina / stats.maxStamina;
            shape.setColor(Color.GOLD);
            shape.rect(startX, staminaStartY, barWidth * staminaPercent, barHeight);
        }

        // Znacznik gracza
        if (team == Team.PLAYERS) {
            shape.setColor(Color.WHITE);
            shape.rect((startX + (barWidth / 2) - singWidth / 2), startY + (2 * barHeight) + (2 * spacing), singWidth, singHeight);
        }
    }

    // NARZĘDZIA I FIZYKA
    public void push(float forceX, float forceY) {
        position.x += forceX;
        position.y += forceY;

        if (position.x < screenWidthStart) position.x = screenWidthStart;
        if (position.x > screenWidthEnd - stats.width) position.x = screenWidthEnd - stats.width;
        if (position.y < screenHeightStart) position.y = screenHeightStart;
        if (position.y > screenHeightEnd - stats.height) position.y = screenHeightEnd - stats.height;

        hurtBox.setPosition(position.x, position.y);
    }

    public void syncHurtBox() {
        this.hurtBox.x = position.x - hurtBox.width / 2;
        this.hurtBox.y = position.y - hurtBox.height / 2;
    }

    //  GETTERY I SETTERY
    public Rectangle getHitBox() { return hitBox; }
    public Rectangle getHurtBox() { return hurtBox; }
    public FighterStats getStats() { return stats; }
    public boolean hasHitTarget() { return hasHitTarget; }
    public void setHasHitTarget(boolean value) { this.hasHitTarget = value; }
    public Vector2 getFacingDirection() { return facingDirection; }
    public FighterState getFighterState() { return state; }
    public Team getTeam() { return team; }
    public Vector2 getPosition() { return position; }
    public float getCurrentStamina() { return currentStamina; }
    public FighterController getController() { return controller; }
    public float getCurrentHealth() { return currentHealth; }
    public float getScreenWidthEnd() { return screenWidthEnd; }
    public float getScreenHeightEnd() { return screenHeightEnd; }
}
