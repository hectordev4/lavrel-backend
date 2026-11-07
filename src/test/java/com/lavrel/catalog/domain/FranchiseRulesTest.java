package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.domain.GameMetadata.Creator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FranchiseRulesTest {

    private static boolean rejected(String franchise, List<String> devs, List<String> pubs) {
        return FranchiseRules.rejectionReason(franchise, devs, pubs, Set.of("eaplay")).isPresent();
    }

    @Test
    void studioNamesAreNotFranchises() {
        assertThat(rejected("Team17 Digital", List.of("Mouldy Toof Studios"), List.of("Team17"))).isTrue();
        assertThat(rejected("CAPCOM", List.of("CAPCOM Co., Ltd."), List.of())).isTrue();
        assertThat(rejected("Hothead Games", List.of("Hothead Games Inc."), List.of())).isTrue();
    }

    @Test
    void placeholdersAndBlockedBrandsAreNotFranchises() {
        assertThat(rejected("FRANCHISE", List.of(), List.of())).isTrue();
        assertThat(rejected("EA Play", List.of("DICE"), List.of("Electronic Arts"))).isTrue();
        assertThat(rejected("  ", List.of(), List.of())).isTrue();
    }

    @Test
    void realFranchisesStay() {
        assertThat(rejected("Monster Hunter", List.of("CAPCOM Co., Ltd."), List.of("CAPCOM Co., Ltd."))).isFalse();
        assertThat(rejected("BioShock", List.of("2K Boston"), List.of("2K"))).isFalse();
        assertThat(rejected("Warhammer", List.of("Creative Assembly"), List.of("SEGA"))).isFalse();
        assertThat(rejected("Pokémon", List.of("Game Freak"), List.of("Nintendo"))).isFalse();
    }

    @Test
    void keysIgnoreCaseAccentsAndSymbols() {
        assertThat(FranchiseRules.key("EA Play")).isEqualTo("eaplay");
        assertThat(FranchiseRules.key("Pokémon™")).isEqualTo("pokemon");
    }

    @Test
    void importKeepsOnlyRealOnes() {
        List<Creator> kept = FranchiseRules.keepReal(
                List.of(new Creator("Team17 Digital", 1L), new Creator("Worms", 2L)),
                List.of(new Creator("Team17", null)), List.of(new Creator("Team17", null)), Set.of());
        assertThat(kept).extracting(Creator::name).containsExactly("Worms");
    }
}
