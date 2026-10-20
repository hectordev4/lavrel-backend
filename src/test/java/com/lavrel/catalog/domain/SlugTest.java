package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugTest {

    @Test
    void turnsATitleIntoReadableLowercaseWords() {
        assertThat(Slug.of("The Witcher 3: Wild Hunt — Remastered")).isEqualTo("the-witcher-3-wild-hunt-remastered");
        assertThat(Slug.of("Half-Life 2")).isEqualTo("half-life-2");
        assertThat(Slug.of("DOOM Eternal")).isEqualTo("doom-eternal");
    }

    @Test
    void removesAccents() {
        assertThat(Slug.of("Pokémon Légendes")).isEqualTo("pokemon-legendes");
        assertThat(Slug.of("Ni no Kuni™: Wrath of the White Witch")).isEqualTo("ni-no-kuni-wrath-of-the-white-witch");
    }

    @Test
    void collapsesPunctuationAndTrimsTheEnds() {
        assertThat(Slug.of("  --Hello,   World!!  ")).isEqualTo("hello-world");
    }

    @Test
    void neverReturnsAnEmptySlug() {
        assertThat(Slug.of("ゲーム")).isEqualTo("game");
        assertThat(Slug.of("!!!")).isEqualTo("game");
        assertThat(Slug.of(null)).isEqualTo("game");
    }

    @Test
    void longTitlesAreCutWithoutLeavingATrailingDash() {
        String slug = Slug.of("a ".repeat(100));
        assertThat(slug.length()).isLessThanOrEqualTo(60);
        assertThat(slug).doesNotEndWith("-").doesNotStartWith("-");
    }
}
