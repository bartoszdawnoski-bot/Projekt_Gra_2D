package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

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

    //GRAFIKI I ANIMACJE
    public TextureRegion idleFrame;
    public Animation<TextureRegion> runAnim = null;
    public Animation<TextureRegion> punchAnim = null;
    public Animation<TextureRegion> sprintAnim = null;

    // PAMIĘĆ PODRĘCZNA ANIMACJI
    private static final HashMap<String, TextureRegion> idleCache = new HashMap<>();
    private static final HashMap<String, Animation<TextureRegion>> runCache = new HashMap<>();
    private static final HashMap<String, Animation<TextureRegion>> punchCache = new HashMap<>();
    private static final HashMap<String, Animation<TextureRegion>> sprintCache = new HashMap<>();

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

    public Fighter(Vector2 position, FighterController controller, FighterStats stats, Team team, String clubName, Boolean isBoss) {
        this.position.set(position);
        this.controller = controller;
        this.stats = stats;
        this.team = team;
        this.isBoss = isBoss;

        currentHealth = stats.maxHealth;
        currentStamina = stats.maxStamina;

        hurtBox.set(position.x, position.y, stats.width, stats.height);
        hitBox.set(position.x, position.y, 0f, 0f);

        loadSkin(clubName);
    }

    private void loadSkin(String clubName) {
        String lowerClub = clubName.toLowerCase();

        if (idleCache.containsKey(lowerClub)) {
            this.idleFrame = idleCache.get(lowerClub);
            this.runAnim = runCache.get(lowerClub);
            this.punchAnim = punchCache.get(lowerClub);
            this.sprintAnim = sprintCache.get(lowerClub);
            return;
        }

        com.badlogic.gdx.utils.Array<TextureRegion> rFrames = new com.badlogic.gdx.utils.Array<>();
        com.badlogic.gdx.utils.Array<TextureRegion> pFrames = new com.badlogic.gdx.utils.Array<>();

        if (lowerClub.equals("gl")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_0003.png"))));
        }
        else if (lowerClub.equals("kato")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac_Katowice_0001.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Katowice_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Katowice_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Katowice_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Katowice_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Katowice_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Katowice_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Katowice_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Katowice_0003.png"))));
        }
        else if (lowerClub.equals("chuje")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac_Ruch_0001.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Ruch_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Ruch_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Ruch_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Ruch_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Ruch_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Ruch_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Ruch_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Ruch_0003.png"))));
        }
        else if (lowerClub.equals("zab")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac_Zabole_0001.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Zabole_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Zabole_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Zabole_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Zabole_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Zabole_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Zabole_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Zabole_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Zabole_0003.png"))));
        }
        else if (lowerClub.equals("kfc")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac_KFC_0001.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_KFC_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_KFC_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_KFC_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_KFC_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_KFC_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_KFC_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_KFC_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_KFC_0003.png"))));
        }
        else if (lowerClub.equals("sosna")) {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac_Gorole_0001.png")));

            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Gorole_0001.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Gorole_0002.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Gorole_0003.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Gorole_0004.png"))));
            rFrames.add(new TextureRegion(new Texture(Gdx.files.internal("Bieg_2_Gorole_0005.png"))));

            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Gorole_0001.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Gorole_0002.png"))));
            pFrames.add(new TextureRegion(new Texture(Gdx.files.internal("cios_1_Gorole_0003.png"))));
        }
        else {
            this.idleFrame = new TextureRegion(new Texture(Gdx.files.internal("Postac.png")));
        }

        this.runAnim = rFrames.isEmpty() ? null : new Animation<>(0.1f, rFrames);
        this.punchAnim = pFrames.isEmpty() ? null : new Animation<>(0.08f, pFrames);
        this.sprintAnim = rFrames.isEmpty() ? null : new Animation<>(0.06f, rFrames);

        idleCache.put(lowerClub, this.idleFrame);
        runCache.put(lowerClub, this.runAnim);
        punchCache.put(lowerClub, this.punchAnim);
        sprintCache.put(lowerClub, this.sprintAnim);
    }

    public TextureRegion getActiveFrame() {
        if (state == FighterState.MOVING && runAnim != null) return runAnim.getKeyFrame(stateTimer, true);
        if ((state == FighterState.ATTACKING || state == FighterState.STRONG_ATTACKING) && punchAnim != null) return punchAnim.getKeyFrame(stateTimer, false);
        if (state == FighterState.SPRINTING && sprintAnim != null) return sprintAnim.getKeyFrame(stateTimer, true);
        return idleFrame;
    }

    public void update(float delta) {
        stateTimer += delta;

        if (hitCooldown > 0) hitCooldown -= delta;

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
            }
        } else if(isDebuffAmphetamineActive) {
            amphetamineDebufTimer -= delta;
            if(amphetamineDebufTimer <= 0){
                isDebuffAmphetamineActive = false;
                stats.staminaRegen *= 2f;
                stats.speed *= 2f;
                stats.sprintSpeed *= 2f;
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
                if (state != FighterState.DEAD) {
                    state = FighterState.DEAD;
                    stateTimer = 0f;
                }
                currentHealth = 0;
            }

            if(fireTimer <= 0f){
                isFireActive = false;
                fireTickTimer = 0f;
            }
        }

        switch (state) {
            case IDLE:
            case MOVING: {
                Vector2 moveInput = controller.getMovementDirection();
                float inputLen = moveInput.len();

                if (controller.wantsToDodge() && currentStamina > stats.dodgeCost) {
                    state = FighterState.DODGING;
                    if(!isAmphetamineActive) currentStamina -= stats.dodgeCost;
                    stateTimer = 0f;
                    if (!moveInput.isZero()) velocity.set(moveInput).nor().scl(stats.speed * 1.2f);
                    else velocity.set(-facingDirection.x * stats.speed * 0.8f, 0);
                }
                else if (currentStamina > stats.attackCost && controller.wantsToAttack()) {
                    state = FighterState.ATTACKING;
                    if(!isAmphetamineActive) currentStamina -= stats.attackCost;
                    stateTimer = 0f;
                    hasHitTarget = false;
                }
                else if (currentStamina > stats.strongAttackCost && controller.wantsToStrongAttack()) {
                    state = FighterState.STRONG_ATTACKING;
                    if(!isAmphetamineActive) currentStamina -= stats.strongAttackCost;
                    stateTimer = 0f;
                    hasHitTarget = false;
                }
                else if (currentStamina > 1f && controller.wantsToSprint() && inputLen > 0.20f) {
                    if (state != FighterState.SPRINTING) state = FighterState.SPRINTING;
                }
                else if (controller.wantsToBlock() && currentStamina >= 30f) {
                    if (state != FighterState.BLOCKING) state = FighterState.BLOCKING;
                }
                else {
                    if (state == FighterState.IDLE && inputLen > 0.20f) {
                        state = FighterState.MOVING;
                    }
                    else if (state == FighterState.MOVING && inputLen < 0.08f) {
                        state = FighterState.IDLE;
                    }

                    if (state == FighterState.MOVING) {
                        float inputMagnitude = Math.min(1f, inputLen);
                        velocity.add(moveInput.x * stats.acceleration * delta, moveInput.y * stats.acceleration * delta);
                        velocity.limit(stats.speed * inputMagnitude);

                        if (Math.abs(moveInput.x) > 0.1f) {
                            facingDirection.set(moveInput.x > 0 ? 1 : -1, 0);
                        }
                    } else {
                        velocity.scl(stats.friction);
                        if (velocity.len() < 5f) velocity.setZero();
                    }
                }

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
                    stateTimer = 0f;
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
                    stateTimer = 0f;
                }
                break;
            }
            case DODGING: {
                velocity.scl(0.98f);
                if (stateTimer < 0.2f) hurtBox.set(0, 0, 0, 0);
                else hurtBox.set(position.x, position.y, stats.width, stats.height);

                if (stateTimer > 0.3f) {
                    state = FighterState.IDLE;
                    stateTimer = 0f;
                    velocity.setZero();
                }
                break;
            }
            // --- POPRAWIONY BLOK WYJŚCIA ZE SPRINTU (FIX NA STATE THRASHING) ---
            case SPRINTING: {
                Vector2 moveInput = controller.getMovementDirection();
                float inputLen = moveInput.len();

                if (currentStamina <= 0 || !controller.wantsToSprint() || moveInput.isZero()) {
                    // Zamiast wymuszać IDLE i bezwarunkowo zerować czas, sprawdzamy intencję ruchu
                    if (moveInput.isZero() || inputLen < 0.08f) {
                        state = FighterState.IDLE;
                    } else {
                        state = FighterState.MOVING;
                    }
                    // USUNIĘTO: stateTimer = 0f; -> Dzięki temu zachowujemy ciągłość czasu klatek lokomocji
                } else {
                    currentStamina -= stats.sprintCost * delta;
                    velocity.add(moveInput.x * stats.sprintAcceleration * delta, moveInput.y * stats.sprintAcceleration * delta);
                    velocity.limit(stats.sprintSpeed);
                    facingDirection.set(moveInput).nor();
                }
                break;
            }
            // --- POPRAWIONY BLOK WYJŚCIA Z BLOKOWANIA ---
            case BLOCKING: {
                Vector2 moveInput = controller.getMovementDirection();
                float inputLen = moveInput.len();

                if (!controller.wantsToBlock() || currentStamina <= 0) {
                    if (moveInput.isZero() || inputLen < 0.08f) {
                        state = FighterState.IDLE;
                    } else {
                        state = FighterState.MOVING;
                    }
                    isBlocking = false;
                    // USUNIĘTO: stateTimer = 0f; -> Zapobiega rwaniu klatek po nagłym opuszczeniu gardy w ruchu
                } else {
                    isBlocking = true;
                    currentStamina -= 5f * delta;

                    if (!moveInput.isZero()) {
                        float inputMagnitude = Math.min(1f, inputLen);
                        velocity.add(moveInput.x * stats.acceleration/2 * delta, moveInput.y * stats.acceleration/2 * delta);
                        velocity.limit(stats.speed * inputMagnitude);
                        facingDirection.set(moveInput).nor();
                        if (moveInput.x != 0) facingDirection.set(moveInput.x > 0 ? 1 : -1, 0);
                    } else {
                        velocity.scl(stats.friction);
                        if (velocity.len() < 5f) velocity.setZero();
                    }
                }
                break;
            }
            case STAGGERED: {
                velocity.scl(0.9f);
                hitBox.set(0, 0, 0, 0);

                if (stateTimer > 0.8f) {
                    state = FighterState.IDLE;
                    stateTimer = 0f;
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

        position.add(velocity.x * delta, velocity.y * delta);

        if (position.x < screenWidthStart) { position.x = screenWidthStart; velocity.x = 0; }
        if (position.y < screenHeightStart) { position.y = screenHeightStart; velocity.y = 0; }
        if (position.x > screenWidthEnd - stats.width) { position.x = screenWidthEnd - stats.width; velocity.x = 0; }
        if (position.y > screenHeightEnd - stats.height) { position.y = screenHeightEnd - stats.height; velocity.y = 0; }

        hurtBox.setPosition(position.x, position.y);
    }

    public void applyDamage(float damageAmount) {
        if (isAmphetamineActive) damageAmount *= 0.5f;
        if (currentArmor > 0) {
            currentArmor -= damageAmount;
            if (currentArmor < 0) { currentHealth += currentArmor; currentArmor = 0; }
        } else {
            currentHealth -= damageAmount;
        }
        if (currentHealth < 0) currentHealth = 0;
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
            currentStamina -= isStrongDamage ? staminaDamage * 3 : staminaDamage;

            if (currentStamina <= 0) {
                currentStamina = 0; state = FighterState.STAGGERED; stateTimer = 0f;
                currentHealth -= damage * 0.2f;
                velocity.x = knockbackDirX * finalKnockback; velocity.y = knockbackDirY * finalKnockback;
                hitCooldown = INVULNERABILITY_TIME;
            }
        } else {
            applyDamage(damage);
            state = FighterState.STAGGERED; stateTimer = 0f;
            velocity.x = knockbackDirX * finalKnockback; velocity.y = knockbackDirY * finalKnockback;
            hitCooldown = INVULNERABILITY_TIME;
            if (currentHealth <= 0) state = FighterState.DEAD;
        }
    }

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
        if(this.controller instanceof AIInput) {
            if(this.state == FighterState.DEAD) return;

            TextureRegion currentFrame = getActiveFrame();
            float spriteH = (currentFrame != null) ? currentFrame.getRegionHeight() : stats.height;
            float topOfCharacter = Math.max(position.y + stats.height, position.y - 15f + spriteH);

            float drawX = position.x;
            float drawY = topOfCharacter + 35f;

            if(((AIInput) this.controller).getCurrentRole() == Role.AGRESSOR) font.draw(batch, "AGRESSOR", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.FLANKER) font.draw(batch, "FLANKER", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.STALLER) font.draw(batch, "STALER", drawX, drawY);
            else if (((AIInput) this.controller).getCurrentRole() == Role.WATCHER) font.draw(batch, "WATCHER", drawX, drawY);
        }
    }

    public void drawStatusBars(ShapeRenderer shape) {
        if (state == FighterState.DEAD) return;
        if(controller instanceof PlayerInput) return;

        float barWidth = Math.max(40f, stats.width * 1.2f);
        float barHeight = 6f;
        float spacing = 3f;

        float startX = position.x + (stats.width / 2f) - (barWidth / 2f);

        TextureRegion currentFrame = getActiveFrame();
        float spriteH = (currentFrame != null) ? currentFrame.getRegionHeight() : stats.height;
        float topOfCharacter = Math.max(position.y + stats.height, position.y - 15f + spriteH);

        float startY = topOfCharacter + 12f;

        float buffBoxSize = 10f;
        float buffSpacing = 3f;

        // HP
        shape.setColor(Color.BLACK);
        shape.rect(startX - 1, startY - 1, barWidth + 2, barHeight + 2);
        shape.setColor(Color.FIREBRICK);
        shape.rect(startX, startY, barWidth, barHeight);
        float healthPercent = currentHealth / stats.maxHealth;
        shape.setColor(Color.valueOf("#32CD32"));
        shape.rect(startX, startY, barWidth * healthPercent, barHeight);

        // STAMINA
        float currentBuffY = startY + barHeight + spacing;
        if (currentStamina < stats.maxStamina) {
            float staminaStartY = startY - barHeight - spacing;
            shape.setColor(Color.BLACK);
            shape.rect(startX - 1, staminaStartY - 1, barWidth + 2, barHeight + 2);
            shape.setColor(Color.DARK_GRAY);
            shape.rect(startX, staminaStartY, barWidth, barHeight);
            float staminaPercent = currentStamina / stats.maxStamina;
            shape.setColor(Color.valueOf("#FFD700"));
            shape.rect(startX, staminaStartY, barWidth * staminaPercent, barHeight);
        }

        // IKONY BUFFÓW
        float currentBuffX = startX;
        if (isFireActive && ((fireTimer > 3f) || ((fireTimer * 5f) % 1f > 0.5f))) {
            shape.setColor(Color.BLACK); shape.rect(currentBuffX - 1, currentBuffY - 1, buffBoxSize + 2, buffBoxSize + 2);
            shape.setColor(Color.FIREBRICK); shape.rect(currentBuffX, currentBuffY, buffBoxSize, buffBoxSize);
            currentBuffX += (buffBoxSize + buffSpacing);
        }
        if (isAmphetamineActive && ((amphetamineTimer > 3f) || ((amphetamineTimer * 5f) % 1f > 0.5f))) {
            shape.setColor(Color.BLACK); shape.rect(currentBuffX - 1, currentBuffY - 1, buffBoxSize + 2, buffBoxSize + 2);
            shape.setColor(Color.PURPLE); shape.rect(currentBuffX, currentBuffY, buffBoxSize, buffBoxSize);
            currentBuffX += (buffBoxSize + buffSpacing);
        }
        if (isDebuffAmphetamineActive && ((amphetamineDebufTimer > 3f) || ((amphetamineDebufTimer * 5f) % 1f > 0.5f))) {
            shape.setColor(Color.BLACK); shape.rect(currentBuffX - 1, currentBuffY - 1, buffBoxSize + 2, buffBoxSize + 2);
            shape.setColor(Color.GRAY); shape.rect(currentBuffX, currentBuffY, buffBoxSize, buffBoxSize);
            currentBuffX += (buffBoxSize + buffSpacing);
        }

        // ZNACZNIK SOJUSZNIKA
        if (team == Team.PLAYERS && controller instanceof AIInput) {
            float indicatorWidth = 14f, indicatorHeight = 4f;
            float indX = position.x + (stats.width / 2f) - (indicatorWidth / 2f);
            float indY = currentBuffY + buffBoxSize + spacing;
            shape.setColor(Color.BLACK); shape.rect(indX - 1, indY - 1, indicatorWidth + 2, indicatorHeight + 2);
            shape.setColor(Color.WHITE); shape.rect(indX, indY, indicatorWidth, indicatorHeight);
        }
    }

    public void drawSprite(SpriteBatch batch) {
        if(state == FighterState.DEAD) return;
        TextureRegion currentFrame = getActiveFrame();
        if (currentFrame == null) return;

        float baseWidth = idleFrame.getRegionWidth();
        float spriteWidth = currentFrame.getRegionWidth();
        float spriteHeight = currentFrame.getRegionHeight();

        float drawX = position.x + (stats.width - baseWidth) / 2f;
        float drawY = position.y - 15f;

        if (facingDirection.x < 0) batch.draw(currentFrame, drawX + baseWidth, drawY, -spriteWidth, spriteHeight);
        else batch.draw(currentFrame, drawX, drawY, spriteWidth, spriteHeight);
    }

    public void push(float forceX, float forceY) {
        position.x += forceX; position.y += forceY;
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

    public void heal(float healAmount) {
        currentHealth += healAmount;
        if (currentHealth > stats.maxHealth) currentHealth = stats.maxHealth;
    }

    public void applyAmphetamineBuff(float duration) { this.isAmphetamineActive = true; this.amphetamineTimer = duration; }
    public void applyFireDebuff(float duration) { this.isFireActive = true; this.fireTimer = duration; }
    public void equipConsumable(int slotIndex, ConsumableItem item) { if (slotIndex >= 0 && slotIndex < 3) quickSlots[slotIndex] = item; }
    public void setActiveSlot(int slotIndex) { if (slotIndex >= 0 && slotIndex < 3) activeSlotIndex = slotIndex; }
    public void useActiveItem(Fighter target) { ConsumableItem currentItem = quickSlots[activeSlotIndex]; if (currentItem != null) currentItem.tryUse(this, target); }
    public void shootFlare(float impactDamage) {
        float startX = (facingDirection.x > 0) ? position.x + stats.width : position.x - 20f;
        float startY = position.y + (stats.height / 2f);
        MainGame.activeProjectiles.add(new Projectile(startX, startY, facingDirection.x * 600f, this.team, impactDamage, true, 100f));
    }

    public void setEquippedWeapon(Weapon weapon) { this.equippedWeapon = weapon; if(weapon != null) weapon.equipTo(this.stats); }
    public Weapon getEquippedWeapon(){return equippedWeapon;}
    public void equipArmor(Armor head, Armor torso) { this.headArmor = head; this.torsoArmor = torso; this.maxArmor = (head != null ? head.getArmorPoints() : 0) + (torso != null ? torso.getArmorPoints() : 0); this.currentArmor = this.maxArmor; }
    public float getCurrentArmor() { return currentArmor; }
    public float getMaxArmor() { return maxArmor; }
    public void setActiveSlotIndex(int index) { if (index >= 0 && index < 3) this.activeSlotIndex = index; }
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
    public void setMapBounds(float startX, float endX, float startY, float endY) { this.screenWidthStart = startX; this.screenWidthEnd = endX; this.screenHeightStart = startY; this.screenHeightEnd = endY; }
}
