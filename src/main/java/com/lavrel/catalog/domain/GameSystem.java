package com.lavrel.catalog.domain;

/**
 * Where a game is sold and played: the family of systems, not the generation (PlayStation covers PS4 and PS5).
 * Not the same as Platform, which is about accounts a user can link: a system can exist without any account to link
 * (Nintendo Switch, later). PC is filled automatically for every game from Steam; Xbox and PlayStation are set by an
 * admin until their adapters fill them.
 */
public enum GameSystem {
    PC, XBOX, PLAYSTATION
}
