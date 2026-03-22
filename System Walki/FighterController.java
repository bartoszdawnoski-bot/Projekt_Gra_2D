package com.walka.kibolgrad;

import com.badlogic.gdx.math.Vector2;

public interface FighterController {
    boolean wantsToAttack();
    boolean wantsToStrongAttack();
    boolean wantsToDodge();
    boolean wantsToBlock();
    boolean wantsToSprint();
    Vector2 getMovementDirection();
}
