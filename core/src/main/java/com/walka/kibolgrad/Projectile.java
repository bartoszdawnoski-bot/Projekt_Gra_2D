package com.walka.kibolgrad;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Projectile {
    public Vector2 position;
    public float speedX;
    public Rectangle hitBox;
    public Team ownerTeam;
    public float damage;
    public float lifeTime = 2f;
    public float explosionRange = 0f;
    public boolean isDestroyed = false;
    public boolean isExplosive = false;

    public Projectile(float x, float y, float speedX, Team team, float damage, boolean isExplosive, float explosionRange) {
        this.position = new Vector2(x, y);
        this.speedX = speedX;
        this.ownerTeam = team;
        this.damage = damage;
        this.isExplosive = isExplosive;
        this.explosionRange = explosionRange;
        this.hitBox = new Rectangle(x, y, 20f, 10f);
    }

    public void update(float delta) {
        position.x += speedX * delta;
        hitBox.setPosition(position.x, position.y);

        lifeTime -= delta;
        if (lifeTime <= 0) {
            isDestroyed = true;
        }
    }
}
