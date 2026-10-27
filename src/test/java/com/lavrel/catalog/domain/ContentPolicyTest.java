package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.config.CatalogFilterProperties;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContentPolicyTest {

    private static final long SEXUAL_CONTENT = 12095;
    private static final long NUDITY = 6650;
    private static final long PUZZLE = 1664;
    private static final long MATCH_3 = 1665;

    private final ContentPolicy policy = new ContentPolicy(new CatalogFilterProperties(
            true, "game", Set.of(3), Set.of(4), 3, Set.of(SEXUAL_CONTENT, NUDITY), 5, 0.20, 3,
            Set.of(3799L, MATCH_3, 9130L, 6653L), Set.of("Visual Novel", "Match 3", "Hentai")));

    private static GameSignals game(List<Integer> descriptors, List<TagWeight> tags) {
        return new GameSignals(1, "game", descriptors, tags, List.of());
    }

    private static TagWeight t(long id, int weight) {
        return new TagWeight(id, null, weight);
    }

    @Test
    void descriptor3RejectsRegardlessOfTags() {
        assertThat(policy.evaluate(game(List.of(3), List.of(t(19, 5000))))).isPresent();
    }

    @Test
    void descriptor4WithSexualTagInTopThreeRejectsEvenAtSmallShare() {
        List<TagWeight> tags = List.of(t(19, 1000), t(21, 990), t(NUDITY, 100), t(122, 90), t(1695, 80));
        assertThat(policy.evaluate(game(List.of(4), tags)))
                .hasValueSatisfying(r -> assertThat(r).contains("descriptor 4"));
    }

    @Test
    void descriptor4WithSexualTagAtRankFourPasses() {
        List<TagWeight> tags = List.of(t(19, 1000), t(21, 990), t(122, 980), t(NUDITY, 970), t(1695, 960));
        assertThat(policy.evaluate(game(List.of(2, 4, 5), tags))).isEmpty();
    }

    @Test
    void descriptorsWithoutSexualTagsPass() {
        assertThat(policy.evaluate(game(List.of(1, 2, 4, 5), List.of(t(19, 5000))))).isEmpty();
    }

    @Test
    void dlcIsRejected() {
        assertThat(policy.evaluate(new GameSignals(1, "dlc", List.of(), List.of(), List.of()))).isPresent();
    }

    @Test
    void witcher3RealTagsPass() {
        List<TagWeight> witcher = List.of(t(1695, 1156), t(122, 973), t(1742, 904), t(4166, 616), t(1684, 598),
                t(21, 584), t(4182, 572), t(NUDITY, 567), t(6426, 522), t(1697, 424), t(1756, 424), t(4172, 418),
                t(19, 398), t(6971, 358), t(4231, 356), t(4604, 342), t(4057, 336), t(4342, 202), t(3810, 123),
                t(SEXUAL_CONTENT, 116));
        assertThat(policy.evaluate(game(List.of(1, 5), witcher))).isEmpty();
    }

    @Test
    void sexualTagAtRankOneIsAlwaysRejected() {
        // Rank 1 always holds at least 20% of the top 5.
        List<TagWeight> tags = List.of(t(SEXUAL_CONTENT, 1000), t(19, 990), t(21, 980), t(122, 970), t(1695, 960));
        assertThat(policy.evaluate(game(List.of(), tags))).hasValueSatisfying(r -> assertThat(r).contains("rank 1"));
    }

    @Test
    void sexualTagAtRankThreeBelowTwentyPercentPasses() {
        List<TagWeight> tags = List.of(t(19, 1000), t(21, 1000), t(NUDITY, 300), t(122, 250), t(1695, 200));
        assertThat(policy.evaluate(game(List.of(), tags))).isEmpty();
    }

    @Test
    void sexualTagAtRankFourPasses() {
        List<TagWeight> tags = List.of(t(19, 320), t(21, 310), t(122, 305), t(SEXUAL_CONTENT, 300), t(1695, 10));
        assertThat(policy.evaluate(game(List.of(), tags))).isEmpty();
    }

    @Test
    void puzzleGamesPass() {
        assertThat(policy.evaluate(game(List.of(), List.of(t(PUZZLE, 1000), t(19, 900), t(21, 800))))).isEmpty();
    }

    @Test
    void storyRichGamesPass() {
        assertThat(policy.evaluate(game(List.of(), List.of(t(3798, 1000), t(19, 900), t(21, 800))))).isEmpty();
    }

    @Test
    void visualNovelTagRejects() {
        assertThat(policy.evaluate(game(List.of(), List.of(t(19, 1000), t(21, 900), t(3799, 50))))).isPresent();
    }

    @Test
    void match3TagRejectsAtAnyRank() {
        assertThat(policy.evaluate(game(List.of(), List.of(t(19, 1000), t(21, 900), t(MATCH_3, 10))))).isPresent();
    }

    @Test
    void excludedGenreRejects() {
        assertThat(policy.evaluate(new GameSignals(1, "game", List.of(), List.of(), List.of("Visual Novel"))))
                .isPresent();
    }
}
