package com.walka.kibolgrad;

import com.badlogic.gdx.math.Vector2;

public interface AiLogicModule {
    Vector2 getMovement(Fighter self, Fighter target, Vector2 targetPoint);
    boolean wantsToAttack(Fighter self, Fighter target);
    boolean wantsToStrongAttack(Fighter self, Fighter target);
    boolean wantsToBlock(Fighter self, Fighter target);
    boolean wantsToDodge(Fighter self, Fighter target);
    boolean wantsToSprint(Fighter self, Fighter target, Vector2 targetPoint);
}
