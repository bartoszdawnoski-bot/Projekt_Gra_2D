package com.walka.kibolgrad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class BattleDirector {
    //REFERENCJE
    private Array<Fighter> FighterRegistry = new Array<>();
    private ObjectMap<Fighter, Array<Fighter>> FighterMap = new ObjectMap<>();

    //PARAMETRY
    private float Difficulty = 0.01f; // maly = duza trudnosc, czeste odwiezanie
    private float currentTime = 0f;

    // WEKTORY GLOBALNE
    private final Vector2 orderPoint = new Vector2();

    public BattleDirector(Array<Fighter> f) {
        this.FighterRegistry = f;
    }

    public void update(float delta) {
        currentTime += delta;
        if (currentTime >= Difficulty) {
            assignRole();
            currentTime = 0f;
        }
    }

    public void assignRole() {
        // Sprzątanie martwych
        for (int i = FighterRegistry.size - 1; i >= 0; i--) {
            if (FighterRegistry.get(i).getFighterState() == FighterState.DEAD) {
                FighterRegistry.removeIndex(i);
            }
        }
        FighterMap.clear();

        // Przydział celów dla AI
        for (int j = 0; j < FighterRegistry.size; j++) {
            Fighter f = FighterRegistry.get(j);

            if (!(f.getController() instanceof AIInput)) continue;

            AIInput ai = (AIInput) f.getController();
            float bestScore = -999999f;
            Fighter bestTarget = null;
            boolean isTerrified = false;

            // Punktacja wrogów
            for (int k = 0; k < FighterRegistry.size; k++) {
                Fighter f2 = FighterRegistry.get(k);
                float score = 1000;
                boolean isCurrentTerrified = false;

                if (f2 != f && f.getTeam() != f2.getTeam()) {
                    float dist = f.getPosition().dst(f2.getPosition());
                    float myPower = f.getStats().attackDamage + f.getStats().strongAttackDamage;
                    float enemyPower = f2.getStats().attackDamage + f2.getStats().strongAttackDamage;

                    score -= dist;

                    // Aggro na gracza
                    if (!(f2.getController() instanceof AIInput)) {
                        score += 10000f;
                    }

                    // Lojalność celu
                    if (ai.getTarget() == f2) {
                        score += 500f;
                    }

                    // Status HP i Stagger
                    score += f2.getStats().maxHealth - f2.getCurrentHealth();
                    if (f2.getFighterState() == FighterState.STAGGERED) score += 500;

                    // Kalkulacja siły
                    if (enemyPower > myPower) {
                        score -= (enemyPower - myPower) * 5;
                        if (enemyPower - myPower > 50) {
                            isCurrentTerrified = true;
                        }
                    } else {
                        score += (myPower - enemyPower) * 5;
                    }

                    // Kalkulacja zasięgu
                    if (f2.getStats().range > f.getStats().range) {
                        score -= (f2.getStats().range - f.getStats().range);
                        isCurrentTerrified = true;
                    } else {
                        score += (f.getStats().range - f2.getStats().range);
                    }

                    // Kalkulacja staminy
                    if (f2.getCurrentStamina() > f.getCurrentStamina() && (f.getCurrentStamina() / f.getStats().maxStamina) < 0.3f && (f2.getCurrentStamina() / f2.getStats().maxStamina) > 0.5f) {
                        score /= 2;
                    }

                    // Kara za tłok
                    if (FighterMap.containsKey(f2)) {
                        int attackersCount = FighterMap.get(f2).size;
                        if (ai.getTarget() != f2) {
                            if (attackersCount == 1) score -= 1000f;
                            if (attackersCount >= 2) score -= 10000f;
                        }
                    }

                    // Aktualizacja faworyta
                    if (score > bestScore) {
                        bestScore = score;
                        bestTarget = f2;
                        isTerrified = isCurrentTerrified;
                    }
                }
            }

            if (bestTarget == null) continue;

            if (!FighterMap.containsKey(bestTarget)) {
                FighterMap.put(bestTarget, new Array<>());
            }

            Array<Fighter> attackers = FighterMap.get(bestTarget);
            orderPoint.set(bestTarget.getPosition());
            Role assignedRole;
            boolean token = false;

            float side = (f.getPosition().x < bestTarget.getPosition().x) ? -1f : 1f;
            float attackDist = 35f;

            if (isTerrified) {
                assignedRole = Role.STALLER;
                orderPoint.x += side * 150f;
            } else if (attackers.size == 0) {
                assignedRole = Role.AGRESSOR;
                orderPoint.x += side * attackDist;
                token = true;
            } else if (attackers.size == 1) {
                assignedRole = Role.FLANKER;
                orderPoint.x += (-side) * attackDist;
                token = true;
            } else if (attackers.size == 2) {
                assignedRole = Role.FLANKER;
                orderPoint.y += 60f;
                token = true;
            } else {
                assignedRole = Role.WATCHER;
                token = false;

                float angle = (attackers.size * 60f) * MathUtils.degRad;
                orderPoint.x += MathUtils.cos(angle) * 850f * side;
                orderPoint.y += MathUtils.sin(angle) * 800f;
            }

            // Wydanie rozkazu
            ai.setOrder(bestTarget, assignedRole, orderPoint, token);
            attackers.add(f);
        }
    }
}
