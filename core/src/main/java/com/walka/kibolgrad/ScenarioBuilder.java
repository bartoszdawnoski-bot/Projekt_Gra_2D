package com.walka.kibolgrad;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class ScenarioBuilder {
    public static BattleConfig createOriginalFight(Texture spriteTexture) {
        BattleConfig config = new BattleConfig()
            .setMap("Oryginalne Boisko")
            .setMapImage("background.png")
            .setMapBounds(543f, 1382f, 162f, 918f);;

        FighterStats playerStats = new FighterStats(20, 5, 10, 20);

        Fighter player = new Fighter(new Vector2(150, 500), new PlayerInput(), playerStats, Team.PLAYERS, spriteTexture, true);

        Weapon bat = new Weapon("Kij Bejsbolowy", 1.5f, 40f, 2f);
        Armor kurtka = new Armor("Skorzana Kurtka", 60f);
        Armor kask = new Armor("Kask Bokserski", 40f);

        player.setEquippedWeapon(bat);
        player.equipArmor(kask, kurtka);
        player.equipConsumable(0, new MedKit(3, 50f));
        player.equipConsumable(1, new Speed(2, 15f));  // Speed = Amfetamina
        player.equipConsumable(2, new Flare(10, 40));

        config.addPlayerTeamMember(player);

        FighterStats ally1Stats = new FighterStats(5, 20, 5, 10);
        Fighter ally1 = new Fighter(new Vector2(400, 800), new AIInput(null), ally1Stats, Team.PLAYERS, spriteTexture, false);
        config.addPlayerTeamMember(ally1);

        FighterStats ally2Stats = new FighterStats(20, 5, 10, 20);
        Fighter ally2 = new Fighter(new Vector2(400, 600), new AIInput(null), ally2Stats, Team.PLAYERS, spriteTexture, false);
        config.addPlayerTeamMember(ally2);

        FighterStats enemy1Stats = new FighterStats(5, 20, 5, 10);
        Fighter enemy1 = new Fighter(new Vector2(1000, 800), new AIInput(null), enemy1Stats, Team.ENEMIES, spriteTexture, false);
        config.addEnemyTeamMember(enemy1);

        FighterStats enemy2Stats = new FighterStats(20, 5, 10, 20);
        Fighter enemy2 = new Fighter(new Vector2(1000, 600), new AIInput(null), enemy2Stats, Team.ENEMIES, spriteTexture, false);
        config.addEnemyTeamMember(enemy2);

        FighterStats enemy3Stats = new FighterStats(5, 20, 5, 10);
        Fighter enemy3 = new Fighter(new Vector2(1000, 400), new AIInput(null), enemy3Stats, Team.ENEMIES, spriteTexture, false);
        config.addEnemyTeamMember(enemy3);

        return config;
    }
}
