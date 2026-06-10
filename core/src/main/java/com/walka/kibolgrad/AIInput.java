package com.walka.kibolgrad;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class AIInput implements FighterController {
    private Fighter self;
    private Array<Fighter> fighters;
    private Fighter target;

    private AiLogicModule currentBrain;
    private final Aggressor aggressorLogic = new Aggressor();
    private final Flanker flankerLogic = new Flanker();
    private final Watcher watcherLogic = new Watcher();
    private final Staller stallerLogic = new Staller();

    private Role currentRole;
    private boolean hasAttackToken = false;
    private final Vector2 targetPoint = new Vector2();

    private final Vector2 movementVector = new Vector2();
    private final Vector2 separation = new Vector2();
    private final Vector2 tempPush = new Vector2();

    public AIInput(Array<Fighter> fighters) {
        this.fighters = fighters;
        this.currentBrain = aggressorLogic;
    }

    public void setSelf(Fighter self) { this.self = self; }
    public void setAllFighters(Array<Fighter> fighters) { this.fighters = fighters; }
    public Fighter getTarget() { return target; }
    public Role getCurrentRole() { return currentRole; }

    public void setOrder(Fighter target, Role role, Vector2 targetPoint, boolean hasAttackToken) {
        this.target = target;
        this.targetPoint.set(targetPoint);
        this.hasAttackToken = hasAttackToken;

        if (this.currentRole != role || this.currentBrain == null) {
            this.currentRole = role;
            switch (role) {
                case AGRESSOR: this.currentBrain = aggressorLogic; break;
                case FLANKER: this.currentBrain = flankerLogic; break;
                case WATCHER: this.currentBrain = watcherLogic; break;
                case STALLER: this.currentBrain = stallerLogic; break;
            }
        }
    }

    @Override
    public boolean wantsToAttack() {
        if (!hasAttackToken || DevManager.freezeBots) return false;
        return currentBrain.wantsToAttack(self, target);
    }

    @Override
    public boolean wantsToStrongAttack() {
        if (!hasAttackToken || DevManager.freezeBots) return false;
        return currentBrain.wantsToStrongAttack(self, target);
    }

    @Override
    public boolean wantsToDodge() { return DevManager.freezeBots ? false : currentBrain.wantsToDodge(self, target); }
    @Override
    public boolean wantsToBlock() { return DevManager.freezeBots ? false : currentBrain.wantsToBlock(self, target); }
    @Override
    public boolean wantsToSprint() { return DevManager.freezeBots ? false : currentBrain.wantsToSprint(self, target, targetPoint); }

    @Override
    public Vector2 getMovementDirection() {
        movementVector.setZero();
        if (target == null || self == null || target.getFighterState() == FighterState.DEAD || DevManager.freezeBots) {
            return movementVector;
        }

        self.getFacingDirection().x = (target.getPosition().x > self.getPosition().x) ? 1 : -1;

        Vector2 baseMove = currentBrain.getMovement(self, target, targetPoint);

        // --- ZMODYFIKOWANY SĄSIEDZKI ALGORYTM SEPARATION ---
        separation.setZero();
        float dRange = 65f;

        for (int i = 0; i < fighters.size; i++) {
            Fighter f = fighters.get(i);
            if (f != self && f != target && f.getFighterState() != FighterState.DEAD) {
                float d = self.getPosition().dst(f.getPosition());
                if (d < dRange && d > 1f) {
                    tempPush.set(self.getPosition()).sub(f.getPosition()).nor();
                    float weight = (dRange - d) / dRange;
                    // Progresywne dodawanie siły omijania bez natychmiastowej normalizacji do 1.0
                    separation.add(tempPush.scl(weight * 3.5f));
                }
            }
        }

        float MARGIN = 60f;
        if (self.getPosition().x < self.getScreenWidthStart() + MARGIN) separation.add(2f, 0);
        else if (self.getPosition().x > self.getScreenWidthEnd() - self.getStats().width - MARGIN) separation.add(-2f, 0);

        if (self.getPosition().y < self.getScreenHeightStart() + MARGIN) separation.add(0, 2f);
        else if (self.getPosition().y > self.getScreenHeightEnd() - self.getStats().height - MARGIN) separation.add(0, -2f);

        // Łączenie wektora dążenia taktycznego z fizyką omijania tłumu
        if (baseMove.isZero()) {
            movementVector.set(separation);
        } else {
            movementVector.set(baseMove).scl(0.7f).add(separation.scl(0.3f));
        }

        // Dopiero na samym końcu ograniczamy wektor wypadkowy do maksymalnej dopuszczalnej wartości
        if (movementVector.len() > 1.0f) {
            movementVector.nor();
        }

        // Ścisła strefa histerezy — odrzuca mikroskopijne drgania poniżej 0.15
        if (movementVector.len() < 0.15f) {
            movementVector.setZero();
        }

        return movementVector;
    }
}
