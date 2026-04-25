package com.walka.kibolgrad;

import com.badlogic.gdx.utils.Array;

public class BattleConfig {
    public String mapName = "default_map";

    public String mapImagePath = "background.png";
    public float minX = 543f, maxX = 1382f, minY = 162f, maxY = 918f;

    public Array<Fighter> teamPlayers = new Array<>();
    public Array<Fighter> teamEnemies = new Array<>();

    public BattleListener listener = null;


    public BattleConfig setMap(String mapName) {
        this.mapName = mapName;
        return this;
    }

    public BattleConfig setMapImage(String imagePath) {
        this.mapImagePath = imagePath;
        return this;
    }

    public BattleConfig setMapBounds(float minX, float maxX, float minY, float maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        return this;
    }

    public BattleConfig addPlayerTeamMember(Fighter f) {
        this.teamPlayers.add(f);
        return this;
    }

    public BattleConfig addEnemyTeamMember(Fighter f) {
        this.teamEnemies.add(f);
        return this;
    }

    public BattleConfig setListener(BattleListener listener) {
        this.listener = listener;
        return this;
    }
}
