package com.walka.kibolgrad;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ExplosionEffect {
    private float x, y;
    private float maxRadius;
    private float lifeTime;
    private float currentTimer = 0f;
    public boolean isFinished = false;

    public ExplosionEffect(float x, float y, float maxRadius, float lifeTime) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.lifeTime = lifeTime;
    }

    public void update(float delta) {
        currentTimer += delta;
        if (currentTimer >= lifeTime) {
            isFinished = true;
        }
    }

    public void draw(ShapeRenderer shapeRenderer) {
        float progress = currentTimer / lifeTime;

        float currentRadius = maxRadius * progress;

        shapeRenderer.setColor(1f, 1f - progress, 0f, 0.3f);

        shapeRenderer.circle(x, y, currentRadius);
    }
}
