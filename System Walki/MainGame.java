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
    private Array<Fighter> playersFighters;
    private Array<Fighter> enemiesFighters;
    private Array<AIInput> playersAI;
    private Array<AIInput> enemiesAI;
    private BattleDirector director;

    private float enemiesNum = 5;
    private float playersNum = 5;

    public void checkBodyPush(Fighter f1, Fighter f2) {
        if(f1.getHurtBox().overlaps(f2.getHurtBox())) {
            float dx = (f1.getHurtBox().x + f1.getHurtBox().width/2) - (f2.getHurtBox().x + f2.getHurtBox().width/2);
            float dy = (f1.getHurtBox().y + f1.getHurtBox().height/2) - (f2.getHurtBox().y + f2.getHurtBox().height/2);
            Vector2 pushDir = new Vector2(dx, dy).nor();
            if(pushDir.len() == 0)pushDir.set(1,0);
            float softPush = 1.0f;

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
            };

        }
    }

    public void checkCombat(Fighter attacker, Fighter victim) {
        if(attacker.getHitBox().width > 0 && attacker.getHitBox().overlaps(victim.getHurtBox()) && !attacker.hasHitTarget()) {
            if(attacker.getFighterState() == FighterState.ATTACKING) {
                victim.takeDamage(attacker.getStats().attackDamage,attacker.getStats().attackDamage * 0.5f, attacker.getFacingDirection().x, false);
            } else if(attacker.getFighterState() == FighterState.STRONG_ATTACKING){
                victim.takeDamage(attacker.getStats().strongAttackDamage,attacker.getStats().strongAttackDamage * 0.5f, attacker.getFacingDirection().x * 1.5f, true);
            }
            attacker.getHitBox().set(0,0,0,0);
            attacker.setHasHitTarget(true);
        }
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        allFighters = new Array<>();
        playersFighters = new Array<>();
        enemiesFighters = new Array<>();
        playersAI = new Array<>();
        enemiesAI = new Array<>();

        FighterStats playerStats = new FighterStats(20, 5, 15, 30);
        FighterStats enemyStats = new FighterStats(8, 25, 5, 10);

        for(int i = 1; i <= playersNum; i++) {
            playersAI.add(new AIInput(allFighters));
            playersFighters.add(new Fighter(new Vector2(100,1000 - (i*200)), playersAI.get(i - 1), (i%2==0) ? playerStats : enemyStats, Team.PLAYERS));
            playersAI.get(i - 1).setSelf(playersFighters.get(i-1));
        }

        for(int i = 1; i <= enemiesNum; i++) {
            enemiesAI.add(new AIInput(allFighters));
            enemiesFighters.add(new Fighter(new Vector2(1000,1000 - (i*200)), enemiesAI.get(i - 1), (i%2==0) ? playerStats : enemyStats, Team.ENEMIES));
            enemiesAI.get(i - 1).setSelf(enemiesFighters.get(i - 1));
        }

        playersFighters.add(new Fighter(new Vector2(150,500), new PlayerInput(), playerStats, Team.PLAYERS));

        for(Fighter fighter : playersFighters) {
            allFighters.add(fighter);
        }

        for(Fighter fighter : enemiesFighters) {
            allFighters.add(fighter);
        }

        director = new BattleDirector(allFighters);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        director.update(delta);
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
