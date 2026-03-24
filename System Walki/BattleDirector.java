package com.walka.kibolgrad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class BattleDirector {
    // REFERENCJE
    private Array<Fighter> FighterRegistry;
    private ObjectMap<Fighter, Array<Fighter>> FighterMap = new ObjectMap<>();

    // PARAMETRY
    private float updateInterval = 0.25f;
    private float currentTime = 0f;

    // WEKTORY GLOBALNE
    private final Vector2 orderPoint = new Vector2();

    public BattleDirector(Array<Fighter> f) {
        this.FighterRegistry = f;
    }

    public void update(float delta) {
        currentTime += delta;
        if (currentTime >= updateInterval) {
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
                float score = 1000f;
                boolean isCurrentTerrified = false;

                if (f2 != f && f.getTeam() != f2.getTeam()) {
                    float dist = f.getPosition().dst(f2.getPosition());
                    float myPower = f.getStats().attackDamage + f.getStats().strongAttackDamage;
                    float enemyPower = f2.getStats().attackDamage + f2.getStats().strongAttackDamage;

                    // Zmniejszony wpływ dystansu, żeby zapobiec ciągłym zmianom celu
                    score -= (dist * 2f);

                    // Aggro na gracza
                    if (!(f2.getController() instanceof AIInput)) {
                        score += 5000f;
                    }

                    if (ai.getTarget() == f2) {
                        score += 2000f;
                    }

                    score += f2.getStats().maxHealth - f2.getCurrentHealth();
                    if (f2.getFighterState() == FighterState.STAGGERED) score += 500f;

                    if (enemyPower > myPower) {
                        score -= (enemyPower - myPower) * 5;
                        if (enemyPower - myPower > 50) {
                            isCurrentTerrified = MathUtils.randomBoolean(0.2f);
                        }
                    } else {
                        score += (myPower - enemyPower) * 5;
                    }

                    if (f2.getStats().range > f.getStats().range) {
                        score -= (f2.getStats().range - f.getStats().range);
                        isCurrentTerrified = true;
                    } else {
                        score += (f.getStats().range - f2.getStats().range);
                    }

                    if (f2.getCurrentStamina() > f.getCurrentStamina() && (f.getCurrentStamina() / f.getStats().maxStamina) < 0.3f && (f2.getCurrentStamina() / f2.getStats().maxStamina) > 0.5f) {
                        score *= 0.5f;
                    }

                    if (FighterMap.containsKey(f2)) {
                        int attackersCount = FighterMap.get(f2).size;
                        if (ai.getTarget() != f2) {
                            if (attackersCount == 1) score -= 1500f;
                            if (attackersCount >= 2) score -= 5000f;
                        }
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestTarget = f2;
                        isTerrified = isCurrentTerrified;
                    }
                }
            }

            if (bestTarget == null) {
                ai.setOrder(null, Role.WATCHER, f.getPosition(), false);
                continue;
            }

            if (!FighterMap.containsKey(bestTarget)) {
                FighterMap.put(bestTarget, new Array<Fighter>());
            }

            Array<Fighter> attackers = FighterMap.get(bestTarget);
            orderPoint.set(bestTarget.getPosition());
            Role assignedRole;
            boolean token = false;

            float side = (f.getPosition().x < bestTarget.getPosition().x) ? -1f : 1f;
            float attackDist = 35f;

            if (isTerrified) {
                assignedRole = Role.STALLER;
                orderPoint.x += side * 500f;
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
                float watcherRadius = 200f + (attackers.size * 10f);
                orderPoint.x += MathUtils.cos(angle) * watcherRadius;
                orderPoint.y += MathUtils.sin(angle) * watcherRadius;
            }

            ai.setOrder(bestTarget, assignedRole, orderPoint, token);
            attackers.add(f);
        }
    }
}
