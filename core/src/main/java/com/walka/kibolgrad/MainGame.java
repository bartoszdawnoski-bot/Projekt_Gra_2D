package com.walka.kibolgrad;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainGame extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private Array<Fighter> allFighters;

    public void checkBodyPush(Fighter f1, Fighter f2) {
        if(f1.getHurtBox().overlaps(f2.getHurtBox())) {
            float dx = (f1.getHurtBox().x + f1.getHurtBox().width/2) - (f2.getHurtBox().x + f2.getHurtBox().width/2);
            float dy = (f1.getHurtBox().y + f1.getHurtBox().height/2) - (f2.getHurtBox().y + f2.getHurtBox().height/2);
            Vector2 pushDir = new Vector2(dx, dy).nor();

            float softPush = 0.2f;
            float pWeight = f1.getStats().weight;
            float eWeight = f2.getStats().weight;

            float pRatio = eWeight / (pWeight + eWeight);
            float eRatio = pWeight / (pWeight + eWeight);

            f1.push(pushDir.x * softPush * pRatio, pushDir.y * softPush * pRatio);
            f2.push(-pushDir.x * softPush * eRatio, -pushDir.y * softPush * eRatio);
        }
    }

    public void checkCombat(Fighter attacker, Fighter victim) {
        if(attacker.getHitBox().width > 0 && attacker.getHitBox().overlaps(victim.getHurtBox()) && !attacker.hasHitTarget()) {
            if(attacker.getFighterState() == FighterState.ATTACKING) {
                victim.takeDamage(attacker.getStats().attackDamage,attacker.getStats().attackDamage * 0.5f, attacker.getFacingDirection().x);
            } else if(attacker.getFighterState() == FighterState.STRONG_ATTACKING){
                victim.takeDamage(attacker.getStats().strongAttackDamage,attacker.getStats().strongAttackDamage * 0.5f, attacker.getFacingDirection().x * 1.5f);
            }
            attacker.getHitBox().set(0,0,0,0);
            attacker.setHasHitTarget(true);
        }
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        allFighters = new Array<>();

        FighterStats playerStats = new FighterStats(20, 5, 15, 30);
        FighterStats enemyStats = new FighterStats(8, 25, 5, 10);

        PlayerInput input = new PlayerInput();
        Fighter player = new Fighter(new Vector2(100, 300), input, playerStats, Team.PLAYERS);
        allFighters.add(player);

        AIInput enemyInput = new AIInput(allFighters);
        Fighter enemy = new Fighter(new Vector2(800, 300), enemyInput, enemyStats, Team.ENEMIES);
        enemyInput.setSelf(enemy);
        allFighters.add(enemy);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        for(int i = 0; i < allFighters.size; i++){
            Fighter f = allFighters.get(i);
            f.update(delta);
        }

        for(int i = 0; i < allFighters.size; i++){
            Fighter f1 = allFighters.get(i);
            if(f1.getFighterState() == FighterState.DEAD) continue;

            for (int j = i + 1; j < allFighters.size; j++) {
                Fighter f2 = allFighters.get(j);
                if(f2.getFighterState() == FighterState.DEAD) continue;

                checkBodyPush(f1, f2);
                if(f1.getTeam() != f2.getTeam()) { //frendlyfire off
                    checkCombat(f1, f2);
                    checkCombat(f2, f1);
                }
            }
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for(int i = 0; i < allFighters.size; i++){
            Fighter f = allFighters.get(i);
            f.drawDebug(shapeRenderer);
            f.drawStatusBars(shapeRenderer);
        }
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
