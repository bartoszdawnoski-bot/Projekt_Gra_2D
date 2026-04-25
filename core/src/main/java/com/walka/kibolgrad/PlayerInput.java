package com.walka.kibolgrad;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class PlayerInput implements FighterController{

    @Override
    public boolean wantsToAttack() {
        if (DevManager.isPointerOverUI || MainGame.isHoveringOverUI) return false;
        return Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
    }

    @Override
    public boolean wantsToStrongAttack() { return Gdx.input.isKeyJustPressed(Input.Keys.ALT_LEFT); }

    @Override
    public boolean wantsToDodge() {
        return Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    @Override
    public boolean wantsToBlock() {
        if (DevManager.isPointerOverUI || MainGame.isHoveringOverUI) return false;
        return  Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
    }

    @Override
    public boolean wantsToSprint() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
    }

    @Override
    public Vector2 getMovementDirection() {
        Vector2 vector = new Vector2();
        if(Gdx.input.isKeyPressed(Input.Keys.W)) vector.y += 1;
        if(Gdx.input.isKeyPressed(Input.Keys.S)) vector.y -= 1;
        if(Gdx.input.isKeyPressed(Input.Keys.D)) vector.x += 1;
        if(Gdx.input.isKeyPressed(Input.Keys.A)) vector.x -= 1;
        return vector.nor();
    }
}
