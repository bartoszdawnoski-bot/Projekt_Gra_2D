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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.InputMultiplexer;

import java.util.Comparator;

public class MainGame extends ApplicationAdapter {

    public static class FloatingText {
        public String text;
        public float x, y;
        public Color color;
        public float lifeTime;
        public float maxLifeTime;
        public float scale;
        public float speedY;
        public float speedX;

        public FloatingText(String text, float x, float y, Color color, float scale) {
            this.text = text;
            // Lekki losowy rozrzut na osi X, żeby liczby nie nakładały się na siebie
            this.x = x + MathUtils.random(-25f, 25f);
            this.y = y;
            this.color = new Color(color);
            this.maxLifeTime = 0.8f; // Czas wyświetlania napisu w sekundach
            this.lifeTime = maxLifeTime;
            this.scale = scale;
            this.speedY = 140f; // Szybkość unoszenia się do góry
            this.speedX = MathUtils.random(-30f, 30f); // Delikatne odchylenie w bok
        }

        public void update(float delta) {
            lifeTime -= delta;
            x += speedX * delta;
            y += speedY * delta;
            // Płynne zanikanie (fade out) poprzez proporcjonalną redukcję alfy
            color.a = Math.max(0f, lifeTime / maxLifeTime);
        }
    }
    // RENDEROWANIE
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont myFont;
    private Texture background;
    private OrthographicCamera camera;
    private Viewport viewport;

    // LOGIKA GRY
    private Array<Fighter> allFighters;
    public static Array<Projectile> activeProjectiles;
    public static Array<ExplosionEffect> activeExplosions = new Array<>();
    private Fighter playerFighter;
    private BattleDirector director;
    private BattleConfig config;
    public static Array<FloatingText> activeFloatingTexts = new Array<>();

    // DEV TOOLS & UI
    private Stage devStage;
    private Skin skin;
    private Table devWindow;
    public static boolean isHoveringOverUI = false;

    // ZMIENNE KONTROLNE DEV TOOLS
    public static boolean renderHitboxes = false;
    public static boolean godMode = false;

    // STAŁE SYSTEMOWE
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;
    float widthPlayer = 35f, heightPlayer = 80f;
    float widthPoint = 15f, heightPoint = 15f;

    // POZYCJE UI GRACZA
    float centerX, boxSize = 80f, spacing = 15f, totalWidth;
    float startX, startY = 30f;

    public MainGame () {}
    public MainGame (BattleConfig config) { this.config = config; }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        myFont = new BitmapFont();
        activeFloatingTexts = new Array<>();

        if (this.config == null) {
            this.config = ScenarioBuilder.createOriginalFight();
        }

        background = new Texture(Gdx.files.internal(config.mapImagePath));

        activeProjectiles = new Array<>();
        activeExplosions = new Array<>();
        allFighters = new Array<>();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        centerX = WORLD_WIDTH / 2f;
        totalWidth = (3 * boxSize) + (2 * spacing);
        startX = centerX - (totalWidth / 2f);

        initBattle();
        setupDevTools();
    }

    private void initBattle() {
        System.out.println("Ładowanie mapy: " + config.mapName);
        allFighters.clear();
        activeFloatingTexts.clear();
        allFighters.addAll(config.teamPlayers);
        allFighters.addAll(config.teamEnemies);

        for (Fighter f : allFighters) {
            f.setMapBounds(config.minX, config.maxX, config.minY, config.maxY);
            if (f.getController() instanceof PlayerInput) playerFighter = f;
        }

        for (Fighter f : allFighters) {
            if (f.getController() instanceof AIInput) {
                ((AIInput) f.getController()).setAllFighters(allFighters);
                ((AIInput) f.getController()).setSelf(f);
            }
        }

        director = new BattleDirector(allFighters);
    }

    // MODUŁ WEJŚCIA
    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (config != null && config.listener != null) config.listener.onEscapePressed();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            DevManager.devModeActive = !DevManager.devModeActive;
            if (!DevManager.devModeActive && devStage != null) devStage.setKeyboardFocus(null);
        }

        if (DevManager.devModeActive) {
            Vector2 stagePos = devStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            DevManager.isPointerOverUI = devStage.hit(stagePos.x, stagePos.y, true) != null;
        } else {
            DevManager.isPointerOverUI = false;
        }

        if((DevManager.isSpawningMode || DevManager.isDespawningMode) && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            DevManager.isSpawningMode = false;
            DevManager.isDespawningMode = false;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !DevManager.isPointerOverUI) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (DevManager.isSpawningMode) {
                spawnAtLocation(mouseX - widthPlayer / 2, mouseY - heightPlayer / 2);
                DevManager.isSpawningMode = false;
                if(devStage != null) devStage.setKeyboardFocus(null);
            }
            else if (DevManager.isDespawningMode) {
                despawnAtLocation(mouseX, mouseY);
                DevManager.isDespawningMode = false;
                if(devStage != null) devStage.setKeyboardFocus(null);
            }
        }

        if (playerFighter != null && playerFighter.getFighterState() != FighterState.DEAD) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !DevManager.isPointerOverUI && !DevManager.isSpawningMode && !DevManager.isDespawningMode) {
                com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(mousePos);
                for (int i = 0; i < 3; i++) {
                    float boxX = startX + i * (boxSize + spacing);
                    if (mousePos.x >= boxX && mousePos.x <= boxX + boxSize && mousePos.y >= startY && mousePos.y <= startY + boxSize) {
                        playerFighter.setActiveSlotIndex(i);
                        break;
                    }
                }
            }

            if (!DevManager.isPointerOverUI) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) playerFighter.setActiveSlot(0);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) playerFighter.setActiveSlot(1);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) playerFighter.setActiveSlot(2);

                if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    float mouseX = Gdx.input.getX();
                    float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
                    Fighter selectedTarget = null;
                    for (Fighter f : allFighters) {
                        if (f.getFighterState() != FighterState.DEAD && f.getTeam() == Team.PLAYERS && f.getHurtBox().contains(mouseX, mouseY)) {
                            selectedTarget = f;
                            break;
                        }
                    }
                    playerFighter.useActiveItem(selectedTarget);
                }
            }
        }
    }

    // MODUŁ LOGIKI
    private void updateLogic(float delta) {
        director.update(delta);

        if (godMode && playerFighter != null) {
            playerFighter.heal(9999f);
            playerFighter.equipArmor(new Armor("God Head", 99999f), new Armor("God Torso", 99999f));
        }

        for(int i = 0; i < allFighters.size; i++) {
            allFighters.get(i).update(delta);
        }

        for(int i = 0; i < allFighters.size; i++) {
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

        for (int i = activeProjectiles.size - 1; i >= 0; i--) {
            Projectile p = activeProjectiles.get(i);
            p.update(delta);

            for (Fighter f : allFighters) {
                if (f.getFighterState() != FighterState.DEAD && f.getTeam() != p.ownerTeam) {
                    if (p.hitBox.overlaps(f.getHurtBox())) {
                        f.takeDamage(p.damage, 0, (p.speedX > 0 ? 1 : -1), 0 ,false, null);
                        f.applyFireDebuff(5);
                        p.isDestroyed = true;
                        break;
                    }
                }
            }

            if (p.isDestroyed) {
                if (p.isExplosive) {
                    float expX = p.hitBox.x + (p.hitBox.width / 2f);
                    float expY = p.hitBox.y + (p.hitBox.height / 2f);
                    activeExplosions.add(new ExplosionEffect(expX, expY, p.explosionRange, 0.2f));

                    for(Fighter f2 : allFighters) {
                        if (f2.getFighterState() == FighterState.DEAD) continue;
                        float targetX = f2.getPosition().x + (f2.getStats().width / 2f);
                        float targetY = f2.getPosition().y + (f2.getStats().height / 2f);
                        float distance = Vector2.dst(expX, expY, targetX, targetY);

                        if (distance <= p.explosionRange) {
                            Vector2 pushDir = new Vector2(targetX - expX, targetY - expY).nor();
                            float dmgModifier = 1f - (distance / p.explosionRange);
                            f2.takeDamage(20f * dmgModifier, 0, pushDir.x, pushDir.y, true, null);
                            f2.applyFireDebuff(3);
                        }
                    }
                }
                activeProjectiles.removeIndex(i);
            }
        }

        for (int i = activeExplosions.size - 1; i >= 0; i--) {
            ExplosionEffect effect = activeExplosions.get(i);
            effect.update(delta);
            if (effect.isFinished) activeExplosions.removeIndex(i);
        }

        for (int i = activeFloatingTexts.size - 1; i >= 0; i--) {
            FloatingText ft = activeFloatingTexts.get(i);
            ft.update(delta);
            if (ft.lifeTime <= 0) {
                activeFloatingTexts.removeIndex(i);
            }
        }
    }

    // MODUŁ RYSOWANIA GRAFIKI
    private void drawGraphics() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        allFighters.sort(new Comparator<Fighter>() {
            @Override
            public int compare(Fighter f1, Fighter f2) {
                return Float.compare(f2.getPosition().y, f1.getPosition().y);
            }
        });

        // Tło
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        spriteBatch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        spriteBatch.end();

        // Cienie, Efekty, Hitboxy
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        for (Fighter f : allFighters) {
            if (f.getFighterState() != FighterState.DEAD) {
                float shadowWidth = f.getStats().width * 1.5f;
                float shadowHeight = 15f;
                float shadowX = f.getPosition().x + (f.getStats().width / 2f) - (shadowWidth / 2f);
                float shadowY = f.getPosition().y - (shadowHeight / 2f) - 5;

                shapeRenderer.ellipse(shadowX, shadowY, shadowWidth, shadowHeight);
            }
        }

        // Rysowanie wybuchów
        for (ExplosionEffect effect : activeExplosions) effect.draw(shapeRenderer);

        // Rysowanie Hitboxów i linii intencji
        if (renderHitboxes) {
            for (Fighter f : allFighters) f.drawDebug(shapeRenderer);
        }

        if (DevManager.showIntentions) {
            for (Fighter f : allFighters) {
                if (f.getController() instanceof AIInput) {
                    shapeRenderer.setColor(0, 1, 1, 1);
                    shapeRenderer.rectLine(
                        f.getPosition().x + f.getStats().width / 2, f.getPosition().y + f.getStats().height / 2,
                        f.getPosition().x + f.getStats().width / 2 + (f.getFacingDirection().x * 100),
                        f.getPosition().y + f.getStats().height / 2 + (f.getFacingDirection().y * 100), 2
                    );
                }
            }
        }

        // Duchy z myszki
        if (!DevManager.isPointerOverUI) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (DevManager.isDespawningMode) {
                shapeRenderer.setColor(1f, 0f, 0f, 0.5f);
                shapeRenderer.rect(mouseX - widthPoint / 2, mouseY - heightPoint / 2, widthPoint , heightPoint);
            } else if (DevManager.isSpawningMode) {
                shapeRenderer.setColor(DevManager.spawnAsPlayer ? new Color(0.2f, 0.4f, 1f, 0.5f) : (DevManager.spawnTeam == Team.ENEMIES ? new Color(1f, 0.2f, 0.2f, 0.5f) : new Color(0.2f, 1f, 0.2f, 0.5f)));
                shapeRenderer.rect(mouseX - widthPlayer / 2, mouseY - heightPlayer / 2, widthPlayer , heightPlayer);
            }
        }
        shapeRenderer.end();

        //  Tekstury Postaci
        spriteBatch.begin();
        for(Fighter f : allFighters) f.drawSprite(spriteBatch);
        spriteBatch.end();

        //  Pociski i UI Gracza
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Projectile p : activeProjectiles) {
            shapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
            shapeRenderer.rect(p.hitBox.x, p.hitBox.y, p.hitBox.width, p.hitBox.height);
        }
        for (Fighter f : allFighters) f.drawStatusBars(shapeRenderer);
        drawPlayerUI();
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Teksty nad głowami
        spriteBatch.begin();
        if (renderHitboxes) {
            for (Fighter f : allFighters) f.fontDebug(spriteBatch, myFont);
        }
        for (FloatingText ft : activeFloatingTexts) {
            myFont.setColor(ft.color);
            myFont.getData().setScale(ft.scale);
            myFont.draw(spriteBatch, ft.text, ft.x, ft.y);
        }
        myFont.getData().setScale(1.0f);
        myFont.setColor(Color.WHITE);

        drawPlayerUIText();
        spriteBatch.end();

        // Narzędzia Deweloperskie
        if (DevManager.devModeActive) {
            devStage.act(Gdx.graphics.getDeltaTime());
            devStage.draw();
        }
    }

    private void drawPlayerUI() {
        if (playerFighter == null || playerFighter.getFighterState() == FighterState.DEAD) return;

        for (int i = 0; i < 3; i++) {
            float boxX = startX + i * (boxSize + spacing);
            shapeRenderer.setColor(i == playerFighter.getActiveSlotIndex() ? new Color(0.8f, 0.7f, 0.1f, 1f) : new Color(0.3f, 0.3f, 0.3f, 0.8f));
            shapeRenderer.rect(boxX, startY, boxSize, boxSize);
            shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.9f);
            shapeRenderer.rect(boxX + 3, startY + 3, boxSize - 6, boxSize - 6);
        }

        float hudX = 20f, hudY = WORLD_HEIGHT - 45f, barWidth = 300f, barHeight = 25f;
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
    }

    private void drawPlayerUIText() {
        if (playerFighter == null || playerFighter.getFighterState() == FighterState.DEAD) return;

        String weaponName = playerFighter.getEquippedWeapon() != null ? playerFighter.getEquippedWeapon().getName() : "Piesci";
        myFont.draw(spriteBatch, "Bron: " + weaponName, centerX - 50, startY + boxSize + 25);

        ConsumableItem[] slots = playerFighter.getQuickSlots();
        for (int i = 0; i < 3; i++) {
            float boxX = startX + i * (boxSize + spacing);
            myFont.draw(spriteBatch, "Slot " + (i + 1), boxX + 5, startY + boxSize - 5);
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

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleInput();      // Obsługa wejścia
        updateLogic(delta); // Obliczenia fizyki i walki
        drawGraphics();     // Rysowanie elementów na ekran
    }

    // KREATOR INTERFEJSU PROGRAMISTY
    private void setupDevTools() {
        devStage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        devWindow = new Table(skin);
        devWindow.setBackground("default-window");
        devWindow.top().left().pad(10);
        devWindow.defaults().pad(2).left();

        devWindow.add(new Label("DEV TOOLS", skin)).colspan(2).right().padBottom(10).row();

        // Checkboxy globalne
        final CheckBox cbHitboxes = new CheckBox(" Pokaz Hitboxy", skin);
        //cbHitboxes.setChecked(renderHitboxes);
        cbHitboxes.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { renderHitboxes = cbHitboxes.isChecked(); }
        });
        devWindow.add(cbHitboxes).colspan(2).row();

        final CheckBox cbGodMode = new CheckBox(" God Mode (Player)", skin);
        cbGodMode.setChecked(godMode);
        cbGodMode.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { godMode = cbGodMode.isChecked(); }
        });
        devWindow.add(cbGodMode).colspan(2).padBottom(10).row();

        // Statystyki Spawnowania
        TextField.TextFieldFilter numFilter = (textField, c) -> Character.isDigit(c) || (c == '.' && !textField.getText().contains(".")) || (c == '-' && textField.getText().isEmpty());

        devWindow.add(new Label("STR:", skin));
        TextField tfStr = new TextField(DevManager.strInput, skin); tfStr.setTextFieldFilter(numFilter); devWindow.add(tfStr).width(50).row();

        devWindow.add(new Label("DEX:", skin));
        TextField tfDex = new TextField(DevManager.dexInput, skin); tfDex.setTextFieldFilter(numFilter); devWindow.add(tfDex).width(50).row();

        devWindow.add(new Label("DEF:", skin));
        TextField tfDef = new TextField(DevManager.defInput, skin); tfDef.setTextFieldFilter(numFilter); devWindow.add(tfDef).width(50).row();

        devWindow.add(new Label("WGT:", skin));
        TextField tfWgt = new TextField(DevManager.wgtInput, skin); tfWgt.setTextFieldFilter(numFilter); devWindow.add(tfWgt).width(50).row();

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

        // Kontrola Spawnowania
        final TextButton btnTeam = new TextButton("ENEMIES", skin);
        btnTeam.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                DevManager.spawnTeam = (DevManager.spawnTeam == Team.ENEMIES) ? Team.PLAYERS : Team.ENEMIES;
                btnTeam.setText(DevManager.spawnTeam.toString());
            }
        });
        devWindow.add(btnTeam).colspan(2).fillX().row();

        TextButton btnSpawnBot = new TextButton("Spawn bot", skin);
        btnSpawnBot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { DevManager.isSpawningMode = true; DevManager.spawnAsPlayer = false; DevManager.isDespawningMode = false; }
        });
        devWindow.add(btnSpawnBot).fillX();

        TextButton btnDespawnBot = new TextButton("Despawn", skin);
        btnDespawnBot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { DevManager.isDespawningMode = true; DevManager.isSpawningMode = false; }
        });
        devWindow.add(btnDespawnBot).fillX().row();

        TextButton btnRespawnHero = new TextButton("Spawn player", skin);
        btnRespawnHero.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { DevManager.isSpawningMode = true; DevManager.spawnAsPlayer = true; }
        });
        devWindow.add(btnRespawnHero).colspan(2).fillX().row();

        // Ekwipunek
        devWindow.add(new Label("--- ITEMY ---", skin)).colspan(2).center().padTop(10).row();

        Table itemsTable = new Table(skin);
        TextButton btnGiveMed = new TextButton("Apteczka", skin);
        btnGiveMed.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { if(playerFighter != null) playerFighter.equipConsumable(0, new MedKit(3, 50f)); }
        });
        itemsTable.add(btnGiveMed).fillX().pad(2);

        TextButton btnGiveSpeed = new TextButton("Amfa", skin);
        btnGiveSpeed.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { if(playerFighter != null) playerFighter.equipConsumable(1, new Speed(2, 15f)); }
        });
        itemsTable.add(btnGiveSpeed).fillX().pad(2).row();

        TextButton btnGiveFlare = new TextButton("Raca", skin);
        btnGiveFlare.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { if(playerFighter != null) playerFighter.equipConsumable(2, new Flare(10, 40f)); }
        });
        itemsTable.add(btnGiveFlare).fillX().pad(2);

        TextButton btnGiveBat = new TextButton("Kij", skin);
        btnGiveBat.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { if(playerFighter != null) playerFighter.setEquippedWeapon(new Weapon("Kij Bejsbolowy", 1.5f, 40f, 2f)); }
        });
        itemsTable.add(btnGiveBat).fillX().pad(2);

        devWindow.add(itemsTable).colspan(2).fillX().row();

        // Zabijanie
        TextButton btnKillAll = new TextButton("Zabij wszystkich wrogow", skin);
        btnKillAll.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                for(Fighter f : allFighters) {
                    if(f.getTeam() == Team.ENEMIES) {
                        f.takeDamage(99999f, 0, 0, 0, false, null);
                    }
                }
            }
        });
        devWindow.add(btnKillAll).colspan(2).fillX().padTop(5).row();

        // Opcje AI
        final CheckBox cbLines = new CheckBox(" Linie Intencji", skin);
        cbLines.setChecked(DevManager.showIntentions);
        cbLines.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { DevManager.showIntentions = cbLines.isChecked(); }
        });
        devWindow.add(cbLines).colspan(2).row();

        final CheckBox cbFreeze = new CheckBox(" Zamroz Boty", skin);
        cbFreeze.setChecked(DevManager.freezeBots);
        cbFreeze.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { DevManager.freezeBots = cbFreeze.isChecked(); }
        });
        devWindow.add(cbFreeze).colspan(2).row();

        devWindow.pack();
        devStage.addActor(devWindow);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(devStage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    // NARZĘDZIA POMOCNICZE
    public void checkBodyPush(Fighter f1, Fighter f2) {
        if(f1.getHurtBox().overlaps(f2.getHurtBox())) {
            float dx = (f1.getHurtBox().x + f1.getHurtBox().width/2) - (f2.getHurtBox().x + f2.getHurtBox().width/2);
            float dy = (f1.getHurtBox().y + f1.getHurtBox().height/2) - (f2.getHurtBox().y + f2.getHurtBox().height/2);
            Vector2 pushDir = new Vector2(dx, dy).nor();
            if(pushDir.len() == 0) pushDir.set(1,0);

            float overlapX = (f1.getHurtBox().width / 2 + f2.getHurtBox().width / 2) - Math.abs(dx);
            float overlapY = (f1.getHurtBox().height / 2 + f2.getHurtBox().height / 2) - Math.abs(dy);

            float pRatio = f2.getStats().weight / (f1.getStats().weight + f2.getStats().weight);
            float eRatio = f1.getStats().weight / (f1.getStats().weight + f2.getStats().weight);

            if (overlapX < overlapY) {
                f1.getPosition().x += overlapX * (dx > 0 ? 1 : -1) * pRatio;
                f2.getPosition().x -= overlapX * (dx > 0 ? 1 : -1) * eRatio;
            } else {
                f1.getPosition().y += overlapY * (dy > 0 ? 1 : -1) * pRatio;
                f2.getPosition().y -= overlapY * (dy > 0 ? 1 : -1) * eRatio;
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

    private void spawnAtLocation(float x, float y) {
        float str = getFloatFromInput(DevManager.strInput, 10f);
        float dex = getFloatFromInput(DevManager.dexInput, 10f);
        float def = getFloatFromInput(DevManager.defInput, 10f);
        float wgt = getFloatFromInput(DevManager.wgtInput, 10f);
        FighterStats stats = new FighterStats(str, dex, def, wgt);

        if (DevManager.spawnAsPlayer) {
            for (int i = 0; i < allFighters.size; i++) {
                if (allFighters.get(i).getTeam() == Team.PLAYERS && allFighters.get(i).getController() instanceof PlayerInput) {
                    allFighters.removeIndex(i); break;
                }
            }
            playerFighter = new Fighter(new Vector2(x, y), new PlayerInput(), stats, Team.PLAYERS, "klub1", true);
            playerFighter.setMapBounds(config.minX, config.maxX, config.minY, config.maxY);
            playerFighter.setEquippedWeapon(new Weapon("Kij Bejsbolowy", 1.5f, 40f, 2f));
            playerFighter.equipConsumable(0, new MedKit(3, 50f));
            allFighters.add(playerFighter);
        } else {
            AIInput aiInput = new AIInput(allFighters);
            Fighter bot = new Fighter(new Vector2(x, y), aiInput, stats, DevManager.spawnTeam, "klub1", false);
            bot.setMapBounds(config.minX, config.maxX, config.minY, config.maxY);
            aiInput.setSelf(bot);
            allFighters.add(bot);
        }
    }

    private void despawnAtLocation(float x, float y) {
        for (int i = 0; i < allFighters.size; i++) {
            if(allFighters.get(i).getController() instanceof AIInput && allFighters.get(i).getHurtBox().contains(x, y)) {
                allFighters.removeIndex(i); break;
            }
        }
    }

    private float getFloatFromInput(String input, float defaultValue) {
        try { return Float.parseFloat(input); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (devStage != null) devStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if(shapeRenderer != null) shapeRenderer.dispose();
        if(spriteBatch != null) spriteBatch.dispose();
        if(background != null) background.dispose();
    }
}
