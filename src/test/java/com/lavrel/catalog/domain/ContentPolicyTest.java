package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.domain.ContentPolicy.Level;
import com.lavrel.catalog.domain.ContentPolicy.Signals;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentPolicyTest {

    // Real Steam tag ids
    private static final long HENTAI = 9130;
    private static final long SEXUAL_CONTENT = 12095;
    private static final long NUDITY = 6650;

    private static TagWeight tag(long id, int weight) {
        return new TagWeight(id, null, weight);
    }

    private static ContentPolicy standard() {
        return new ContentPolicy(Level.STANDARD, 5, 0.6);
    }

    private static ContentPolicy strict() {
        return new ContentPolicy(Level.STRICT, 5, 0.6);
    }

    /** The tags and descriptors Steam returned for The Witcher 3 (Nudity sits 8th, Sexual Content last). */
    private static Signals witcher3() {
        return new Signals("The Witcher 3: Wild Hunt", "A dark fantasy open world RPG", List.of(1, 5), List.of("RPG"),
                List.of(tag(1695, 1156), tag(122, 973), tag(1742, 904), tag(4166, 616), tag(1684, 598),
                        tag(21, 584), tag(4182, 572), tag(NUDITY, 567), tag(6426, 522), tag(1697, 424),
                        tag(1756, 424), tag(4172, 418), tag(19, 398), tag(6971, 358), tag(4231, 356),
                        tag(4604, 342), tag(4057, 336), tag(4342, 202), tag(3810, 123), tag(SEXUAL_CONTENT, 116)),
                "m");
    }

    private static Signals monsterHunterWilds() {
        return new Signals("Monster Hunter Wilds", "Hunt monsters", List.of(), List.of("Action", "Adventure", "RPG"),
                List.of(tag(9564, 861), tag(19, 733), tag(3859, 699), tag(3843, 633), tag(1685, 595)), "t");
    }

    private static Signals adultVisualNovel(List<TagWeight> tags) {
        return new Signals("Some Visual Novel", "A story about two people", List.of(), List.of("Casual", "Indie"),
                tags, null);
    }

    @Test
    void witcher3StaysInTheCatalogAtTheStandardLevel() {
        assertThat(standard().rejectionReason(witcher3())).isEmpty();
    }

    @Test
    void witcher3IsBlockedByTheStrictLevel() {
        assertThat(strict().rejectionReason(witcher3())).isPresent();
    }

    @Test
    void anOrdinaryGameIsAllowedEverywhere() {
        assertThat(standard().rejectionReason(monsterHunterWilds())).isEmpty();
        assertThat(strict().rejectionReason(monsterHunterWilds())).isEmpty();
    }

    @Test
    void aVisualNovelWithProminentSexualTagsIsBlocked() {
        var tags = List.of(tag(3799, 800), tag(4085, 700), tag(SEXUAL_CONTENT, 690), tag(NUDITY, 650), tag(4726, 400));
        assertThat(standard().rejectionReason(adultVisualNovel(tags))).isPresent();
    }

    @Test
    void aSexualTagWithAHighShareOfTheTopWeightIsBlockedEvenFarDownTheList() {
        var tags = List.of(tag(1, 1000), tag(2, 990), tag(3, 980), tag(4, 970), tag(5, 960), tag(6, 950),
                tag(SEXUAL_CONTENT, 700));
        assertThat(standard().rejectionReason(adultVisualNovel(tags))).isPresent();
    }

    @Test
    void theHentaiTagBlocksAtAnyWeight() {
        var tags = List.of(tag(1, 1000), tag(2, 900), tag(3, 800), tag(4, 700), tag(5, 600), tag(6, 500),
                tag(7, 400), tag(HENTAI, 30));
        assertThat(standard().rejectionReason(adultVisualNovel(tags))).isPresent();
    }

    @Test
    void adultOnlyAndFrequentSexualContentDescriptorsAlwaysBlock() {
        for (int descriptor : List.of(3, 4)) {
            var signals = new Signals("Game", "text", List.of(descriptor), List.of(), List.of(), null);
            assertThat(standard().rejectionReason(signals)).isPresent();
        }
    }

    @Test
    void esrbAdultsOnlyBlocks() {
        var signals = new Signals("Game", "text", List.of(), List.of(), List.of(), "ao");
        assertThat(standard().rejectionReason(signals)).isPresent();
    }

    @Test
    void sexualGenresAndExplicitWordingBlock() {
        assertThat(standard().rejectionReason(
                new Signals("Game", "text", List.of(), List.of("Indie", "Nudity"), List.of(), null))).isPresent();
        assertThat(standard().rejectionReason(
                new Signals("Sweet Hentai Girls", "text", List.of(), List.of(), List.of(), null))).isPresent();
        assertThat(standard().rejectionReason(
                new Signals("Game", "An NSFW visual novel", List.of(), List.of(), List.of(), null))).isPresent();
    }

    @Test
    void wordMatchingOnlyCatchesWholeWords() {
        assertThat(standard().rejectionReason(
                new Signals("Neurotic Detective", "A quirky mystery", List.of(), List.of(), List.of(), null))).isEmpty();
        assertThat(standard().rejectionReason(
                new Signals("XXXL Tennis", "A big tennis game", List.of(), List.of(), List.of(), null))).isEmpty();
    }

    @Test
    void violenceAloneIsNeverBlocked() {
        var signals = new Signals("Doom", "Rip and tear", List.of(2, 5), List.of("Action"),
                List.of(tag(4345, 900), tag(4667, 800)), "m");
        assertThat(strict().rejectionReason(signals)).isEmpty();
    }

    @Test
    void offAllowsEverything() {
        var signals = new Signals("Hentai", "porn", List.of(3, 4), List.of("Nudity"),
                List.of(tag(HENTAI, 900)), "ao");
        assertThat(new ContentPolicy(Level.OFF, 5, 0.6).rejectionReason(signals)).isEmpty();
    }

    @Test
    void levelParsing() {
        assertThat(Level.parse(null)).isEqualTo(Level.STANDARD);
        assertThat(Level.parse("")).isEqualTo(Level.STANDARD);
        assertThat(Level.parse(" Standard ")).isEqualTo(Level.STANDARD);
        assertThat(Level.parse("OFF")).isEqualTo(Level.OFF);
        assertThat(Level.parse("banana")).isEqualTo(Level.STRICT);
    }
}
