package com.lavrel.assistant.adapter.out.reference;

import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.application.port.out.WikiSourceRepositoryPort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.WikiSource;
import com.lavrel.assistant.domain.WikiText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Looks the achievement up in the game's approved wiki: searches its name, reads the best pages and keeps the part
 * around the achievement. Nothing is stored except the final answer and the page links (the text is fetched live).
 */
@Component
public class WikiReferenceAdapter implements ReferencePort {

    static final int PAGES = 2;
    static final int CHARS_PER_PAGE = 1800;

    private final WikiSourceRepositoryPort sources;
    private final WikiClientPort wiki;

    public WikiReferenceAdapter(WikiSourceRepositoryPort sources, WikiClientPort wiki) {
        this.sources = sources;
        this.wiki = wiki;
    }

    @Override
    public List<Excerpt> find(AchievementContext achievement) {
        Optional<WikiSource> source = sources.find(achievement.gameId());
        if (source.isEmpty()) {
            return List.of();
        }
        WikiSource s = source.get();
        List<Excerpt> excerpts = new ArrayList<>();
        for (String title : wiki.search(s.apiUrl(), achievement.name(), PAGES)) {
            wiki.wikitext(s.apiUrl(), title)
                    .map(WikiText::clean)
                    .filter(text -> !text.isBlank())
                    .map(text -> WikiText.around(text, achievement.name(), CHARS_PER_PAGE))
                    .ifPresent(text -> excerpts.add(new Excerpt(title, s.pageUrl(title), s.license(), text)));
        }
        return excerpts;
    }
}
