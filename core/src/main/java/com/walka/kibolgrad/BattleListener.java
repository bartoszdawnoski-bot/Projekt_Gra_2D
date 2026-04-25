package com.walka.kibolgrad;

public interface BattleListener {
    void onEscapePressed();

    void onBattleEnded(boolean playerWon);
}
