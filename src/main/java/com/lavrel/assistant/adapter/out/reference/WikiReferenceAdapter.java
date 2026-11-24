package com.lavrel.assistant.adapter.out.reference;

import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.SearchTerms;
import com.lavrel.assistant.domain.WikiPages;
import com.lavrel.assistant.domain.ReferenceWiki;
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
    static final int CHARS_PER_PAGE = 3000;
    /** Search results looked at, so pages about this game can be preferred on a franchise wiki. */
    static final int SEARCH_HITS = 6;

    private final ReferenceWikiRepositoryPort wikis;
    private final WikiClientPort wiki;

    public WikiReferenceAdapter(ReferenceWikiRepositoryPort wikis, WikiClientPort wiki) {
        this.wikis = wikis;
        this.wiki = wiki;
    }

    @Override
    public boolean hasSource(long gameId) {
        return wikis.findForGame(gameId).isPresent();
    }

    /** Searches with the English terms and cuts each page around their most telling word. */
    @Override
    public List<Excerpt> findForGame(long gameId, String gameTitle, String searchTerms) {
        Optional<ReferenceWiki> source = wikis.findForGame(gameId);
        if (source.isEmpty()) {
            return List.of();
        }
        ReferenceWiki s = source.get();
        String focus = SearchTerms.focusWord(searchTerms);
        List<Excerpt> excerpts = new ArrayList<>();
        for (String title : WikiPages.preferFor(gameTitle, wiki.search(s.apiUrl(), searchTerms, SEARCH_HITS), PAGES)) {
            wiki.wikitext(s.apiUrl(), title)
                    .map(WikiText::clean)
                    .filter(text -> !text.isBlank())
                    .map(text -> WikiText.around(text, focus, CHARS_PER_PAGE))
                    .ifPresent(text -> excerpts.add(excerpt(s, title, text)));
        }
        return excerpts;
    }

    @Override
    public List<Excerpt> find(AchievementContext achievement) {
        Optional<ReferenceWiki> source = wikis.findForGame(achievement.gameId());
        if (source.isEmpty()) {
            return List.of();
        }
        ReferenceWiki s = source.get();
        List<Excerpt> excerpts = new ArrayList<>();
        List<String> hits = wiki.search(s.apiUrl(), achievement.name(), SEARCH_HITS);
        for (String title : WikiPages.preferFor(achievement.gameTitle(), hits, PAGES)) {
            wiki.wikitext(s.apiUrl(), title)
                    .map(WikiText::clean)
                    .filter(text -> !text.isBlank())
                    .map(text -> WikiText.around(text, achievement.name(), CHARS_PER_PAGE))
                    .ifPresent(text -> excerpts.add(excerpt(s, title, text)));
        }
        return excerpts;
    }

    /** The page with the credit CC BY-SA asks for: link, the wiki's own name and its license. */
    private static Excerpt excerpt(ReferenceWiki wiki, String title, String text) {
        return new Excerpt(title, wiki.pageUrl(title), wiki.siteName(), wiki.license(), wiki.licenseUrl(), text);
    }
}
