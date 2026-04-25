package com.walka.kibolgrad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

public class EnemyInput implements FighterController{

    @Override
    public boolean wantsToAttack() {
        return true;
    }

    @Override
    public boolean wantsToStrongAttack() {return false; }

    @Override
    public boolean wantsToDodge() {
        return false;
    }

    @Override
    public boolean wantsToBlock() {
        return false;
    }

    @Override
    public boolean wantsToSprint() {
        return false;
    }

    @Override
    public Vector2 getMovementDirection() {
        return Vector2.Zero;
    }
}
