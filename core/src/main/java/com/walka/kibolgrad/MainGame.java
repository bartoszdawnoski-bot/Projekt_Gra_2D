package com.walka.kibolgrad;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.InputMultiplexer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainGame extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont myFont;
    private Texture background;
    private Array<Fighter> allFighters;
    public static Array<Projectile> activeProjectiles;
    public static Array<ExplosionEffect> activeExplosions = new Array<>();
    private Fighter playerFighter;
    private BattleDirector director;

    private Stage devStage;
    private Skin skin;
    private Table devWindow;

    private Texture fighterTexture;

    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;
    private OrthographicCamera camera;
    private Viewport viewport;

    float widthPlayer = 35f;
    float heightPlayer = 80f;

    float widthPoint = 15f;
    float heightPoint = 15f;

    public static boolean isHoveringOverUI = false;

    private BattleConfig config;
    public MainGame () {}

    public MainGame (BattleConfig config) {
        this.config = config;
    }

    public void checkBodyPush(Fighter f1, Fighter f2) {
        if(f1.getHurtBox().overlaps(f2.getHurtBox())) {
            float dx = (f1.getHurtBox().x + f1.getHurtBox().width/2) - (f2.getHurtBox().x + f2.getHurtBox().width/2);
            float dy = (f1.getHurtBox().y + f1.getHurtBox().height/2) - (f2.getHurtBox().y + f2.getHurtBox().height/2);
            Vector2 pushDir = new Vector2(dx, dy).nor();
            if(pushDir.len() == 0)pushDir.set(1,0);

            float overlapX = (f1.getHurtBox().width / 2 + f2.getHurtBox().width / 2) - Math.abs(dx);
            float overlapY = (f1.getHurtBox().height / 2 + f2.getHurtBox().height / 2) - Math.abs(dy);

            float pWeight = f1.getStats().weight;
            float eWeight = f2.getStats().weight;
            float pRatio = eWeight / (pWeight + eWeight);
            float eRatio = pWeight / (pWeight + eWeight);

            if (overlapX < overlapY) {
                float sign = (dx > 0) ? 1 : -1;
                float move = overlapX * sign;

                f1.getPosition().x += move * pRatio;
                f2.getPosition().x -= move * eRatio;
            } else {
                float sign = (dy > 0) ? 1 : -1;
                float move = overlapY * sign;

                f1.getPosition().y += move * pRatio;
                f2.getPosition().y -= move * eRatio;
            }

            f1.getHurtBox().setPosition(f1.getPosition().x, f1.getPosition().y);
            f2.getHurtBox().setPosition(f2.getPosition().x, f2.getPosition().y);
            if(!pushDir.isZero()) {
                f1.push(pushDir.x * 0.2f, pushDir.y * 0.2f);
                f2.push(-pushDir.x * 0.2f, -pushDir.y * 0.2f);
            }

        }
    }

    public void checkCombat(Fighter attacker, Fighter victim) {
        if(attacker.getHitBox().width > 0 && attacker.getHitBox().overlaps(victim.getHurtBox()) && !attacker.hasHitTarget()) {
            if(attacker.getFighterState() == FighterState.ATTACKING) {
                victim.takeDamage(attacker.getStats().attackDamage,attacker.getStats().attackDamage * 0.5f, attacker.getFacingDirection().x, 0, false, attacker);
            } else if(attacker.getFighterState() == FighterState.STRONG_ATTACKING){
                victim.takeDamage(attacker.getStats().strongAttackDamage,attacker.getStats().strongAttackDamage * 0.5f, attacker.getFacingDirection().x * 1.5f, 0 ,true, attacker);
            }
            attacker.getHitBox().set(0,0,0,0);
            attacker.setHasHitTarget(true);
        }
    }

    private void handleDevTools() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            DevManager.devModeActive = !DevManager.devModeActive;
            System.out.println("DevMode: " + DevManager.devModeActive);

            if (!DevManager.devModeActive && devStage != null) {
                devStage.setKeyboardFocus(null);
            }
        }
    }

    private float getFloatFromInput(String input, float defaultValue) {
        try {
            return Float.parseFloat(input);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void despawnAtLocation(float x, float y) {
        if(DevManager.isDespawningMode) {
            for (int i = 0; i < allFighters.size; i++) {
                Fighter fighter = allFighters.get(i);
                if(fighter.getController() instanceof AIInput && fighter.getHurtBox().contains(x, y)) {
                    allFighters.removeIndex(i);
                    System.out.println("Despawn in position: " + x + ", " + y);
                    break;
                }
            }
        }
    }

    private void spawnAtLocation(float x, float y) {
        float str = getFloatFromInput(DevManager.strInput, 10f);
        float dex = getFloatFromInput(DevManager.dexInput, 10f);
        float def = getFloatFromInput(DevManager.defInput, 10f);
        float wgt = getFloatFromInput(DevManager.wgtInput, 10f);

        FighterStats stats = new FighterStats(str, dex, def, wgt);

        if (DevManager.spawnAsPlayer) {
            for (int i = 0; i < allFighters.size; i++) {
                Fighter f = allFighters.get(i);
                if (f.getTeam() == Team.PLAYERS && f.getController() instanceof PlayerInput) {
                    allFighters.removeIndex(i);
                    break;
                }
            }

            fighterTexture = new Texture(Gdx.files.internal("Postac.png"));
            Fighter newPlayer = new Fighter(new Vector2(x, y), new PlayerInput(), stats, Team.PLAYERS, fighterTexture, true);

            playerFighter = newPlayer;

            Weapon bat = new Weapon("Kij Bejsbolowy", 1.5f, 40f, 2f);
            playerFighter.setEquippedWeapon(bat);
            playerFighter.equipConsumable(0, new MedKit(3, 50f));

            allFighters.add(playerFighter);
            System.out.println("Player position: " + x + ", " + y);

        } else {
            AIInput aiInput = new AIInput(allFighters);
            Fighter bot = new Fighter(new Vector2(x, y), aiInput, stats, DevManager.spawnTeam, false);
            aiInput.setSelf(bot);
            allFighters.add(bot);
            System.out.println("Bot (" + DevManager.spawnTeam + ") position: " + x + ", " + y);
        }
    }

    private void initBattle() {
        System.out.println("Ładowanie mapy: " + config.mapName);
        allFighters.clear();

        for (Fighter f : config.teamPlayers) {
            allFighters.add(f);
        }

        for (Fighter f : config.teamEnemies) {
            allFighters.add(f);
        }

        for (Fighter f : allFighters) {
            f.setMapBounds(config.minX, config.maxX, config.minY, config.maxY);
        }
    }
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch =  new SpriteBatch();
        myFont = new BitmapFont();
        fighterTexture = new Texture(Gdx.files.internal("Postac.png"));

        if (this.config == null) {
            this.config = ScenarioBuilder.createOriginalFight(fighterTexture);
        }

        background = new Texture(Gdx.files.internal(config.mapImagePath));

        activeProjectiles = new Array<>();
        activeExplosions = new Array<>();
        allFighters = new Array<>();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        if (this.config == null) {
            this.config = ScenarioBuilder.createOriginalFight(fighterTexture);
        }

        initBattle();

        for (Fighter f : allFighters) {
            if (f.getController() instanceof PlayerInput) {
                playerFighter = f;
                break;
            }
        }

        for (Fighter f : allFighters) {
            if (f.getController() instanceof AIInput) {
                ((AIInput) f.getController()).setAllFighters(allFighters);
                ((AIInput) f.getController()).setSelf(f);
            }
        }

        director = new BattleDirector(allFighters);

        devStage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        devWindow = new Table(skin);
        devWindow.setBackground("default-window");
        devWindow.top().left().pad(10);
        devWindow.defaults().pad(2).left();
        devWindow.columnDefaults(0).width(100);

        //Nagłówek
        devWindow.add(new Label("DEV TOOLS", skin)).colspan(2).right().padBottom(10).row();

        TextField.TextFieldFilter numberFilter = new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                if (Character.isDigit(c)) return true;
                if (c == '.' && !textField.getText().contains(".")) return true;
                if (c == '-' && textField.getText().isEmpty()) return true;
                return false;
            }
        };

        //Statystyki
        devWindow.add(new Label("Strength (STR): ", skin)).colspan(2).left().padBottom(10);
        final TextField tfStr = new  TextField(DevManager.strInput, skin);
        tfStr.setTextFieldFilter(numberFilter);
        devWindow.add(tfStr).width(50).row();

        devWindow.add(new Label("Dexterity (DEX): ", skin)).colspan(2).left().padBottom(10);
        final TextField tfDex = new  TextField(DevManager.dexInput, skin);
        tfDex.setTextFieldFilter(numberFilter);
        devWindow.add(tfDex).width(50).row();

        devWindow.add(new Label("Defense (DEF): ", skin)).colspan(2).left().padBottom(10);
        final TextField tfDef = new  TextField(DevManager.defInput, skin);
        tfDef.setTextFieldFilter(numberFilter);
        devWindow.add(tfDef).width(50).row();

        devWindow.add(new Label("Weight (WGT): ", skin)).colspan(2).left().padBottom(10);
        final TextField tfWgt = new  TextField(DevManager.wgtInput, skin);
        tfWgt.setTextFieldFilter(numberFilter);
        devWindow.add(tfWgt).width(50).row();

        //Listeners
        TextField.TextFieldListener statListener = (textField, c) -> {
            DevManager.strInput = tfStr.getText();
            DevManager.dexInput = tfDex.getText();
            DevManager.defInput = tfDef.getText();
            DevManager.wgtInput = tfWgt.getText();
        };

        tfStr.setTextFieldListener(statListener);
        tfDex.setTextFieldListener(statListener);
        tfDef.setTextFieldListener(statListener);
        tfWgt.setTextFieldListener(statListener);

        devWindow.add(new Label("Boot Team: ", skin));
        final TextButton btnTeam = new TextButton("ENEMIES", skin);
        btnTeam.addListener(new ChangeListener() {
           @Override
           public void changed(ChangeEvent event, Actor actor) {
               DevManager.spawnTeam = (DevManager.spawnTeam == Team.ENEMIES) ? Team.PLAYERS : Team.ENEMIES;
               btnTeam.setText(DevManager.spawnTeam.toString());
           }
        });
        devWindow.add(btnTeam).fillX().width(100).row();

        TextButton btnSpawnBot = new  TextButton("Spawn bot", skin);
        btnSpawnBot.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DevManager.isSpawningMode = true;
                DevManager.spawnAsPlayer = false;
                DevManager.isDespawningMode = false;
            }
        });
        devWindow.add(btnSpawnBot).colspan(1).fillX().padTop(10);

        TextButton btnDespawnBot = new  TextButton("Despawn bot", skin);
        btnDespawnBot.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DevManager.isDespawningMode = true;
                DevManager.isSpawningMode = false;
                DevManager.spawnAsPlayer = false;
            }
        });
        devWindow.add(btnDespawnBot).colspan(1).fillX().padTop(10);

        TextButton btnRespawnHero = new TextButton("Spawn player", skin);
        btnRespawnHero.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DevManager.isSpawningMode = true;
                DevManager.spawnAsPlayer = true;
                DevManager.isDespawningMode = false;
            }
        });
        devWindow.add(btnRespawnHero).colspan(2).fillX().padTop(10).row();

        final CheckBox cbLines = new CheckBox("Lines of intention", skin);
        cbLines.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { DevManager.showIntentions = cbLines.isChecked(); }
        });
        devWindow.add(cbLines).colspan(2).left().padBottom(10).padLeft(15);

        final CheckBox cbFreeze = new CheckBox("Freeze bots", skin);
        cbFreeze.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DevManager.freezeBots = cbFreeze.isChecked();
            }
        });
        devWindow.add(cbFreeze).colspan(2).left().padBottom(10).row();

        devWindow.pack();
        devStage.addActor(devWindow);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(devStage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render() {
        // UPDATE I LOGIKA GRY
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        handleDevTools();
        camera.update();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (config != null && config.listener != null) {
                config.listener.onEscapePressed();
            }
        }

        float centerX = MainGame.WORLD_WIDTH / 2f;
        float boxSize = 80f;
        float spacing = 15f;
        float startX = centerX - ((3 * boxSize + 2 * spacing) / 2f);
        float startY = 30f;
        float totalWidth = (3 * boxSize) + (2 * spacing);

        float buffBoxSize = 25f;
        float buffSpacing = 5f;

        float hudX = 20f, hudY = WORLD_HEIGHT - 45f, barWidth = 300f, barHeight = 25f;
        float currentBuffX = hudX;
        float buffBoxY = hudY - 35f - 3 * barHeight;

        if (playerFighter != null && playerFighter.getFighterState() != FighterState.DEAD) {
            if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(mousePos);

                for (int i = 0; i < 3; i++) {
                    float boxX = startX + i * (boxSize + spacing);

                    boolean isMouseInsideX = mousePos.x >= boxX && mousePos.x <= boxX + boxSize;
                    boolean isMouseInsideY = mousePos.y >= startY && mousePos.y <= startY + boxSize;
                    MainGame.isHoveringOverUI =(mousePos.x >= startX && mousePos.x <= startX + totalWidth && mousePos.y >= startY && mousePos.y <= startY + boxSize);
                    if (isMouseInsideX && isMouseInsideY) {
                        playerFighter.setActiveSlotIndex(i);
                        System.out.println("Kliknięto myszką w slot: " + (i + 1));
                        break;
                    }
                }
            }
        }

        if (DevManager.devModeActive) {
            Vector2 stagePos = devStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            DevManager.isPointerOverUI = devStage.hit(stagePos.x, stagePos.y, true) != null;
        } else {
            DevManager.isPointerOverUI = false;
        }

        if((DevManager.isSpawningMode || DevManager.isDespawningMode) && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            System.out.println("Spawning and Despawning mode off");
            DevManager.isSpawningMode = false;
            DevManager.isDespawningMode = false;
        }

        if (DevManager.isSpawningMode && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !DevManager.isPointerOverUI) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            spawnAtLocation(mouseX - widthPlayer / 2, mouseY - heightPlayer / 2);
            DevManager.isSpawningMode = false;
            if(devStage != null) devStage.setKeyboardFocus(null);
        }

        if(DevManager.isDespawningMode && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !DevManager.isPointerOverUI) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            despawnAtLocation(mouseX, mouseY);
            DevManager.isDespawningMode = false;
            if(devStage != null) devStage.setKeyboardFocus(null);
        }

        if (playerFighter != null && playerFighter.getFighterState() != FighterState.DEAD && !DevManager.isPointerOverUI) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) playerFighter.setActiveSlot(0);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) playerFighter.setActiveSlot(1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) playerFighter.setActiveSlot(2);

            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                float mouseX = Gdx.input.getX();
                float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
                Fighter selectedTarget = null;
                for (Fighter f : allFighters) {
                    if (f.getFighterState() != FighterState.DEAD && f.getTeam() == Team.PLAYERS) {
                        if (f.getHurtBox().contains(mouseX, mouseY)) {
                            selectedTarget = f;
                            break;
                        }
                    }
                }
                playerFighter.useActiveItem(selectedTarget);
            }
        }

        float delta = Gdx.graphics.getDeltaTime();
        director.update(delta);

        for(int i = 0; i < allFighters.size; i++){
            allFighters.get(i).update(delta);
        }

        for(int i = 0; i < allFighters.size; i++){
            Fighter f1 = allFighters.get(i);
            if(f1.getFighterState() == FighterState.DEAD) continue;

            for (int j = i + 1; j < allFighters.size; j++) {
                Fighter f2 = allFighters.get(j);
                if(f2.getFighterState() == FighterState.DEAD) continue;

                checkBodyPush(f1, f2);
                if(f1.getTeam() != f2.getTeam()) {
                    checkCombat(f1, f2);
                    checkCombat(f2, f1);
                }
            }
        }

        //AKTUALIZACJA POCISKÓW
        for (int i = MainGame.activeProjectiles.size - 1; i >= 0; i--) {
            Projectile p = MainGame.activeProjectiles.get(i);
            p.update(delta);

            for (Fighter f : allFighters) {
                if (f.getFighterState() != FighterState.DEAD && f.getTeam() != p.ownerTeam) {
                    if (p.hitBox.overlaps(f.getHurtBox())) {
                        f.takeDamage(p.damage, 0, (p.speedX > 0 ? 1 : -1), 0 ,false, null);
                        f.applyFireDebuff(5);
                        p.isDestroyed = true;
                        if (p.isExplosive) {
                            System.out.println("Raca wybuchła! Zasięg: " + p.explosionRange);
                            float explosionCenterX = p.hitBox.x + (p.hitBox.width / 2f);
                            float explosionCenterY = p.hitBox.y + (p.hitBox.height / 2f);
                            int targetsHit = 0;
                            MainGame.activeExplosions.add(new ExplosionEffect(explosionCenterX, explosionCenterY, p.explosionRange, 0.2f));

                            for(Fighter f2 : allFighters) {
                                if (f2.getFighterState() == FighterState.DEAD) continue;
                                if (f2 == f) continue;

                                float targetCenterX = f2.getPosition().x + (f2.getStats().width / 2f);
                                float targetCenterY = f2.getPosition().y + (f2.getStats().height / 2f);

                                float distance = Vector2.dst(explosionCenterX, explosionCenterY, targetCenterX, targetCenterY);

                                if (distance <= p.explosionRange) {
                                    float dirX = targetCenterX - explosionCenterX;
                                    float dirY = targetCenterY - explosionCenterY;
                                    Vector2 pushDir = new Vector2(dirX, dirY).nor();

                                    float damageModifier = 1f - (distance / p.explosionRange);
                                    float finalDamage = 20f * damageModifier;

                                    f2.takeDamage(finalDamage, 0, pushDir.x, pushDir.y, true, null);
                                    f2.applyFireDebuff(3);
                                    targetsHit++;
                                }
                            }
                            System.out.println("Wybuch trafił " + targetsHit);
                        }
                        break;
                    }
                }
            }
            if (p.isDestroyed) {
                MainGame.activeProjectiles.removeIndex(i);
            }
        }
        //TŁO
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        spriteBatch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        spriteBatch.end();

        // HITBOXY I DUCHY
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = MainGame.activeExplosions.size - 1; i >= 0; i--) {
            ExplosionEffect effect = MainGame.activeExplosions.get(i);

            effect.update(delta);
            effect.draw(shapeRenderer);

            if (effect.isFinished) {
                MainGame.activeExplosions.removeIndex(i);
            }
        }

        for(int i = 0; i < allFighters.size; i++){
            Fighter f = allFighters.get(i);
            f.drawDebug(shapeRenderer);

            if(DevManager.showIntentions && f.getController() instanceof AIInput) {
                shapeRenderer.setColor(0, 1, 1, 1);
                shapeRenderer.rectLine(
                    f.getPosition().x + f.getStats().width / 2, f.getPosition().y + f.getStats().height / 2,
                    f.getPosition().x + f.getStats().width / 2 + (f.getFacingDirection().x * 100),
                    f.getPosition().y + f.getStats().height / 2 + (f.getFacingDirection().y * 100), 2
                );
            }
        }

        // Duchy z DevTools
        if (!DevManager.isPointerOverUI) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (DevManager.isDespawningMode) {
                shapeRenderer.setColor(1f, 0f, 0f, 0.5f);
                shapeRenderer.rect(mouseX - widthPoint / 2, mouseY - heightPoint / 2, widthPoint , heightPoint);
            } else if (DevManager.isSpawningMode) {
                if(DevManager.spawnAsPlayer) shapeRenderer.setColor(0.2f, 0.4f, 1f, 0.5f);
                else if (DevManager.spawnTeam == Team.ENEMIES) shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.5f);
                else shapeRenderer.setColor(0.2f, 1f, 0.2f, 0.5f);
                shapeRenderer.rect(mouseX - widthPlayer / 2, mouseY - heightPlayer / 2, widthPlayer , heightPlayer);
            }
        }
        shapeRenderer.end();

        // TEKSTURY POSTACI
        spriteBatch.begin();
        for(int i = 0; i < allFighters.size; i++){
            allFighters.get(i).drawSprite(spriteBatch);
        }
        spriteBatch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Rysowanie aktywnych rac
        for (Projectile p : MainGame.activeProjectiles) {
            shapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
            shapeRenderer.rect(p.hitBox.x, p.hitBox.y, p.hitBox.width, p.hitBox.height);
        }

        // UI, PASKI ZDROWIA
        for(int i = 0; i < allFighters.size; i++){
            allFighters.get(i).drawStatusBars(shapeRenderer);
        }

        if (playerFighter != null && playerFighter.getFighterState() != FighterState.DEAD) {
            // Ekwipunek kwadraty
            for (int i = 0; i < 3; i++) {
                float boxX = startX + i * (boxSize + spacing);
                shapeRenderer.setColor(i == playerFighter.getActiveSlotIndex() ? new Color(0.8f, 0.7f, 0.1f, 1f) : new Color(0.3f, 0.3f, 0.3f, 0.8f));
                shapeRenderer.rect(boxX, startY, boxSize, boxSize);
                shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.9f);
                shapeRenderer.rect(boxX + 3, startY + 3, boxSize - 6, boxSize - 6);
            }

            // Paski stanu gracza
            if (playerFighter.getMaxArmor() > 0) {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
                shapeRenderer.rect(hudX, hudY, barWidth, barHeight);
                shapeRenderer.setColor(Color.ROYAL);
                shapeRenderer.rect(hudX, hudY, barWidth * Math.max(0, playerFighter.getCurrentArmor() / playerFighter.getMaxArmor()), barHeight);
                hudY -= (barHeight + 5f);
            }

            shapeRenderer.setColor(Color.FIREBRICK);
            shapeRenderer.rect(hudX, hudY, barWidth, barHeight);
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.rect(hudX, hudY, barWidth * Math.max(0, playerFighter.getCurrentHealth() / playerFighter.getStats().maxHealth), barHeight);
            hudY -= (barHeight + 5f);

            shapeRenderer.setColor(Color.GRAY);
            shapeRenderer.rect(hudX, hudY, barWidth * 0.8f, 10f);
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.rect(hudX, hudY, (barWidth * 0.8f) * Math.max(0, playerFighter.getCurrentStamina() / playerFighter.getStats().maxStamina), 10f);

            // BUFF
            if (playerFighter.isAmphetamineActive()) {
                float timeLeft = playerFighter.getAmphetamineTimer();
                boolean shouldDraw = (timeLeft > 3f) || ((timeLeft * 5f) % 1f > 0.5f);

                if (shouldDraw) {
                    shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f); // Tło
                    shapeRenderer.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                    shapeRenderer.setColor(Color.PURPLE);
                    shapeRenderer.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                    currentBuffX += (buffBoxSize + buffSpacing);
                }
            }

            // DEBUFF
            if (playerFighter.isAmphetamineDebuffActive()) {
                float timeLeft = playerFighter.getAmphetamineDebuffTimer();
                boolean shouldDraw = (timeLeft > 2f) || ((timeLeft * 5f) % 1f > 0.5f);

                if (shouldDraw) {
                    shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f);
                    shapeRenderer.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                    shapeRenderer.setColor(Color.DARK_GRAY);
                    shapeRenderer.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                    currentBuffX += (buffBoxSize + buffSpacing);
                }
            }

            if (playerFighter.isFireActive()) {
                float timeLeft = playerFighter.getFireTimer();
                boolean shouldDraw = (timeLeft > 2f) || ((timeLeft * 5f) % 1f > 0.5f);

                if (shouldDraw) {
                    shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f);
                    shapeRenderer.rect(currentBuffX, buffBoxY, buffBoxSize, buffBoxSize);

                    shapeRenderer.setColor(Color.FIREBRICK);
                    shapeRenderer.rect(currentBuffX + 2, buffBoxY + 2, buffBoxSize - 4, buffBoxSize - 4);

                    currentBuffX += (buffBoxSize + buffSpacing);
                }
            }
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        //TEKSTY I FONT
        spriteBatch.begin();

        for(int i = 0; i < allFighters.size; i++){
            allFighters.get(i).fontDebug(spriteBatch, myFont);
        }

        if (playerFighter != null && playerFighter.getFighterState() != FighterState.DEAD) {
            String weaponName = playerFighter.getEquippedWeapon() != null ? playerFighter.getEquippedWeapon().getName() : "Piesci";
            myFont.draw(spriteBatch, "Bron: " + weaponName, centerX - 50, startY + boxSize + 25);

            ConsumableItem[] slots = playerFighter.getQuickSlots();
            for (int i = 0; i < 3; i++) {
                float boxX = startX + i * (boxSize + spacing);
                myFont.draw(spriteBatch, "[" + (i + 1) + "]", boxX + 5, startY + boxSize - 5);

                if (slots[i] != null) {
                    String name = slots[i].getName();
                    if(name.length() > 8) name = name.substring(0, 8) + ".";
                    myFont.draw(spriteBatch, name, boxX + 8, startY + boxSize / 2 + 10);
                    myFont.draw(spriteBatch, "x" + slots[i].getCount(), boxX + boxSize / 2 - 10, startY + 20);
                } else {
                    myFont.draw(spriteBatch, "Puste", boxX + 20, startY + boxSize / 2 - 5);
                }
            }
        }

        spriteBatch.end();

        //DEV TOOLS STAGE
        if (DevManager.devModeActive) {
            devStage.act(Gdx.graphics.getDeltaTime());
            devStage.draw();
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        fighterTexture.dispose();
    }
}
