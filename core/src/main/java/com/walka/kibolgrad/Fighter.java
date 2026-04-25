package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Fighter {
    // REFERENCJE
    FighterStats stats;
    Team team;
    FighterController controller;
    Weapon equippedWeapon = null;
    private Fighter lastAttacker = null;

    //STAN I ZEGARY
    private FighterState state = FighterState.IDLE;
    private float stateTimer = 0f;
    private float hitCooldown = 0f;
    final float INVULNERABILITY_TIME = 0.5f;

    private boolean isAmphetamineActive = false;
    private float amphetamineTimer = 0f;
    private boolean isDebuffAmphetamineActive = false;
    private float amphetamineDebufTimer = 0f;

    private boolean isFireActive = false;
    private float fireTimer = 0f;
    private float fireTickTimer = 0f;
    private float aggroTimer = 0f;

    private Boolean isBoss = false;

    //GRAFIKI
    Texture spriteTexture;

    // STATYSTYKI BIEŻĄCE
    private float currentHealth;
    private float currentStamina;
    boolean isBlocking = false;
    private boolean hasHitTarget = false;

    // PANCERZ
    private Armor headArmor;
    private Armor torsoArmor;
    private float maxArmor = 0;
    private float currentArmor = 0;

    //POZYCJA I WEKTORY
    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Vector2 facingDirection = new Vector2(1, 0);

    //HITBOXY
    private final Rectangle hurtBox = new Rectangle();
    private final Rectangle hitBox = new Rectangle();
    float hitHeight = 50f;

    // GRANICE MAPY
    float screenWidthStart = MainGame.WORLD_WIDTH * 0.283f;
    float screenWidthEnd = MainGame.WORLD_WIDTH * 0.72f;

    float screenHeightStart = MainGame.WORLD_HEIGHT * 0.15f;
    float screenHeightEnd = MainGame.WORLD_HEIGHT * 0.85f;

    // EKWIPUNEK
    private ConsumableItem[] quickSlots = new ConsumableItem[3];
    private int activeSlotIndex = 0;

    public Fighter(Vector2 position, FighterController controller, FighterStats stats, Team team, Boolean isBoss) {
        this.position.set(position);
        this.controller = controller;
        this.stats = stats;
        this.team = team;
        this.isBoss = isBoss;
        spriteTexture = null;

        currentHealth = stats.maxHealth;
        currentStamina = stats.maxStamina;

        hurtBox.set(position.x, position.y, stats.width, stats.height);
        hitBox.set(position.x, position.y, 0f, 0f);
    }

    public Fighter(Vector2 position, FighterController controller, FighterStats stats, Team team, Texture texture, Boolean isBoss) {
        this.position.set(position);
        this.controller = controller;
        this.stats = stats;
        this.team = team;
        this.isBoss = isBoss;
        spriteTexture = texture;

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

        if(aggroTimer > 0f) {
            aggroTimer -= delta;
            if (aggroTimer < 0f) {
                lastAttacker = null;
                aggroTimer = 0f;
            }
        }

        if(isAmphetamineActive) {
            amphetamineTimer -= delta;
            currentStamina = stats.maxStamina;

            if (amphetamineTimer <= 0f) {
                isAmphetamineActive = false;
                currentStamina = stats.maxStamina * 0.5f;

                isDebuffAmphetamineActive = true;
                amphetamineDebufTimer = 6f;
                stats.staminaRegen *= 0.5f;
                stats.speed *= 0.50f;
                stats.sprintSpeed *= 0.50f;

                System.out.println("koniec działania Amfetaminy.");
            }
        }else if(isDebuffAmphetamineActive) {
            amphetamineDebufTimer -= delta;

            if(amphetamineDebufTimer <= 0){
                isDebuffAmphetamineActive = false;
                stats.staminaRegen *= 2f;
                stats.speed *= 2f;
                stats.sprintSpeed *= 2f;
                System.out.println("Koniec zejścia.");
            }
        }

        if(isFireActive){
            fireTimer -= delta;
            fireTickTimer += delta;

            if(fireTickTimer >= 1.0f){
                applyDamage(10f);
                fireTickTimer -= 1.0f;
            }

            if(currentHealth <= 0f){
                state = FighterState.DEAD;
                currentHealth = 0;
            }

            if(fireTimer <= 0f){
                isFireActive = false;
                fireTickTimer = 0f;
                System.out.println("koniec ognia.");
            }
        }

        // OBSŁUGA STANÓW
        switch (state) {
            case IDLE:
            case MOVING: {
                Vector2 moveInput = controller.getMovementDirection();

                if (controller.wantsToDodge() && currentStamina > stats.dodgeCost) {
                    state = FighterState.DODGING;
                    if(!isAmphetamineActive) currentStamina -= stats.dodgeCost;
                    stateTimer = 0f;

                    if (!moveInput.isZero()) {
                        velocity.set(moveInput).nor().scl(stats.speed * 1.2f);
                    } else {
                        velocity.set(-facingDirection.x * stats.speed * 0.8f, 0);
                    }
                } else if (currentStamina > stats.attackCost && controller.wantsToAttack()) {
                    state = FighterState.ATTACKING;
                    if(!isAmphetamineActive) currentStamina -= stats.attackCost;
                    stateTimer = 0f;
                    hasHitTarget = false;
                } else if (currentStamina > stats.strongAttackCost && controller.wantsToStrongAttack()) {
                    state = FighterState.STRONG_ATTACKING;
                    if(!isAmphetamineActive) currentStamina -= stats.strongAttackCost;
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
                        float inputMagnitude = Math.min(1f, moveInput.len());
                        velocity.add(moveInput.x * stats.acceleration * delta, moveInput.y * stats.acceleration * delta);
                        velocity.limit(stats.speed * inputMagnitude);
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

                if (stateTimer > stats.attackDuration * 0.2f && stateTimer < stats.attackDuration * 0.5f) {
                    float currentHitWidth = stats.range;
                    float hitX = (facingDirection.x == 1) ? position.x + stats.width : position.x - currentHitWidth;
                    float hitY = position.y + (stats.height / 2f) - (hitHeight / 2f);

                    hitBox.set(hitX, hitY, currentHitWidth, hitHeight);
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

                if (stateTimer > stats.strongAttackDuration * 0.2f && stateTimer < stats.strongAttackDuration * 0.5f) {
                    float currentHitWidth = stats.range;
                    float hitX = (facingDirection.x == 1) ? position.x + stats.width : position.x - currentHitWidth;
                    float hitY = position.y + (stats.height / 2f) - (hitHeight / 2f);

                    hitBox.set(hitX, hitY, currentHitWidth, hitHeight);
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
                        float inputMagnitude = Math.min(1f, moveInput.len());
                        velocity.add(moveInput.x * stats.acceleration/2 * delta, moveInput.y * stats.acceleration/2 * delta);
                        velocity.limit(stats.speed * inputMagnitude);
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

    public void applyDamage(float damageAmount) {

        if (isAmphetamineActive) {
            damageAmount *= 0.5f;
        }
        if (currentArmor > 0) {
            currentArmor -= damageAmount;

            if (currentArmor < 0) {
                currentHealth += currentArmor;
                currentArmor = 0;
                System.out.println("Pancerz zniszczony!");
            }
        }
        else {
            currentHealth -= damageAmount;
        }

        if (currentHealth < 0) {
            currentHealth = 0;
        }
    }

    public void takeDamage(float damage, float staminaDamage, float knockbackDirX, float knockbackDirY, boolean isStrongDamage, Fighter attacker) {
        lastAttacker = attacker;
        aggroTimer = 5.0f;
        if (state == FighterState.DODGING || hitCooldown > 0 || state == FighterState.DEAD) return;
        damage = damage * (1f - stats.damageReduction);

        float incomingKnockback = (attacker != null) ? attacker.getStats().knockbackPower : 1200f;
        if (isStrongDamage) incomingKnockback *= 1.5f;
        float finalKnockback = Math.max(150f, incomingKnockback - (this.stats.weight * 10f));

        if (state == FighterState.BLOCKING && currentStamina > 0) {
            velocity.x = knockbackDirX * (finalKnockback * 0.5f);
            velocity.y = knockbackDirY * (finalKnockback * 0.5f);
            float actualStaminaDamage = isStrongDamage ? staminaDamage * 3 : staminaDamage;
            currentStamina -= actualStaminaDamage;

            if (currentStamina <= 0) {
                currentStamina = 0;
                state = FighterState.STAGGERED;
                stateTimer = 0f;

                currentHealth -= damage * 0.2f;
                velocity.x = knockbackDirX * finalKnockback;
                velocity.y = knockbackDirY * finalKnockback;

                hitCooldown = INVULNERABILITY_TIME;
            }
        } else {
            applyDamage(damage);
            state = FighterState.STAGGERED;
            stateTimer = 0f;
            velocity.x = knockbackDirX * finalKnockback;
            velocity.y = knockbackDirY * finalKnockback;
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

    public void fontDebug(SpriteBatch batch, BitmapFont font) {
        if(this.controller instanceof AIInput)
        {
            if(this.state == FighterState.DEAD) return;
            float drawX = position.x;
            float drawY = position.y;

            if(((AIInput) this.controller).getCurrentRole() == Role.AGRESSOR) font.draw(batch, "AGRESSOR", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.FLANKER) font.draw(batch, "FLANKER", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.STALLER) font.draw(batch, "STALER", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.WATCHER) font.draw(batch, "WATCHER", drawX, drawY);

        } else return;

    }

    public void drawStatusBars(ShapeRenderer shape) {
        if (state == FighterState.DEAD) return;
        if(controller instanceof PlayerInput) return;

        float barWidth = stats.width;
        float barHeight = 5f;
        float spacing = 5f;
        float singHeight = 10f;
        float singWidth = 10f;

        float startX = position.x;
        float startY = position.y + 90f;

        float buffBoxSize = 10f;
        float buffSpacing = 2f;

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

        float buffBoxY = startY + 2*barHeight + 2*spacing;
        float currentBuffX = position.x;

        if (isFireActive) {
            boolean shouldDraw = (fireTimer > 3f) || ((fireTimer * 5f) % 1f > 0.5f);

            if (shouldDraw) {
                shape.setColor(0.1f, 0.1f, 0.1f, 1f); // Tło
                shape.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                shape.setColor(Color.FIREBRICK);
                shape.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                currentBuffX += (buffBoxSize + buffSpacing);
            }
        }

        if (isAmphetamineActive) {
            boolean shouldDraw = (amphetamineTimer > 3f) || ((amphetamineTimer * 5f) % 1f > 0.5f);

            if (shouldDraw) {
                shape.setColor(0.1f, 0.1f, 0.1f, 1f); // Tło
                shape.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                shape.setColor(Color.PURPLE);
                shape.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                currentBuffX += (buffBoxSize + buffSpacing);
            }
        }

        if (isDebuffAmphetamineActive) {
            boolean shouldDraw = (amphetamineDebufTimer > 3f) || ((amphetamineDebufTimer * 5f) % 1f > 0.5f);

            if (shouldDraw) {
                shape.setColor(0.1f, 0.1f, 0.1f, 1f); // Tło
                shape.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                shape.setColor(Color.GRAY);
                shape.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                currentBuffX += (buffBoxSize + buffSpacing);
            }
        }


        // Znacznik gracza
        if (team == Team.PLAYERS) {
            shape.setColor(Color.WHITE);
            shape.rect((startX + (barWidth / 2) - singWidth / 2), startY + (3 * barHeight) + (3 * spacing), singWidth, singHeight);
        }

    }

    public void drawSprite(SpriteBatch batch)
    {
        if(state == FighterState.DEAD || spriteTexture == null) return;

        float spriteWidth = spriteTexture.getWidth();
        float spriteHeight = spriteTexture.getHeight();

        float drawX = position.x + (stats.width - spriteWidth) / 2f;
        float drawY = position.y + (stats.height - spriteHeight) / 2f;

        if (facingDirection.x < 0) {
            batch.draw(spriteTexture, drawX + spriteWidth - 1f, drawY, -spriteWidth, spriteHeight);
        } else {
            batch.draw(spriteTexture, drawX, drawY, spriteWidth, spriteHeight);
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

    //LOGIKA EFEKTÓW
    public void heal(float healAmount) {
        currentHealth += healAmount;
        if (currentHealth > stats.maxHealth) currentHealth = stats.maxHealth;
    }

    public void applyAmphetamineBuff(float duration) {
        this.isAmphetamineActive = true;
        this.amphetamineTimer = duration;
    }

    public void applyFireDebuff(float duration) {
        this.isFireActive = true;
        this.fireTimer = duration;
    }

    //EKWIPUNEK
    public void equipConsumable(int slotIndex, ConsumableItem item) {
        if (slotIndex >= 0 && slotIndex < 3) {
            quickSlots[slotIndex] = item;
        }
    }

    public void setActiveSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 3) {
            activeSlotIndex = slotIndex;
            System.out.println("Wybrano slot: " + (slotIndex + 1));
        }
    }

    public void useActiveItem(Fighter target) {
        ConsumableItem currentItem = quickSlots[activeSlotIndex];
        if (currentItem != null) {
            currentItem.tryUse(this, target);
        } else {
            System.out.println("Pusty slot");
        }
    }

    public void shootFlare(float impactDamage) {
        float startX = (facingDirection.x > 0) ? position.x + stats.width : position.x - 20f;
        float startY = position.y + (stats.height / 2f);

        float projectileSpeed = facingDirection.x * 600f;

        Projectile raca = new Projectile(startX, startY, projectileSpeed, this.team, impactDamage, true, 150f);
        MainGame.activeProjectiles.add(raca);
    }


    //LOGIKA BRONI
    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
        if(weapon != null) {
            weapon.equipTo(this.stats);
        }
    }
    public Weapon getEquippedWeapon(){return equippedWeapon;}

    //ARMOR
    public void equipArmor(Armor head, Armor torso) {
        this.headArmor = head;
        this.torsoArmor = torso;
        this.maxArmor = (head != null ? head.getArmorPoints() : 0) + (torso != null ? torso.getArmorPoints() : 0);
        this.currentArmor = this.maxArmor;
    }

    public float getCurrentArmor() { return currentArmor; }
    public float getMaxArmor() { return maxArmor; }

    public void setActiveSlotIndex(int index) {
        if (index >= 0 && index < 3) {
            this.activeSlotIndex = index;
        }
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
    public float getScreenWidthStart() { return screenWidthStart; }
    public float getScreenHeightStart() {return screenHeightStart; }
    public ConsumableItem[] getQuickSlots() { return quickSlots; }
    public int getActiveSlotIndex() { return activeSlotIndex; }
    public boolean isAmphetamineActive() { return isAmphetamineActive; }
    public float getAmphetamineTimer() { return amphetamineTimer; }
    public float getAmphetamineDebuffTimer() { return amphetamineDebufTimer; }
    public boolean isAmphetamineDebuffActive() {return isDebuffAmphetamineActive; }
    public boolean isFireActive() { return isFireActive; }
    public float getFireTimer() { return fireTimer; }
    public Fighter getLastAttacker() { return lastAttacker; }
    public Boolean isBoss() { return isBoss; }
    public void setMapBounds(float startX, float endX, float startY, float endY) {
        this.screenWidthStart = startX;
        this.screenWidthEnd = endX;
        this.screenHeightStart = startY;
        this.screenHeightEnd = endY;
    }
}
