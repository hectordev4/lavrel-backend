package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameCard;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GamePage;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.out.GameSearchQueryPort;
import com.lavrel.catalog.domain.GameSystem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GameSearchJdbcAdapter implements GameSearchQueryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public GameSearchJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public GamePage search(GameSearchCriteria criteria) {
        GameSearchSql.Query query = GameSearchSql.build(criteria);
        List<GameCard> rows = jdbc.query(query.sql(), query.params(), GameSearchJdbcAdapter::card);
        boolean hasMore = rows.size() > criteria.size();
        return new GamePage(hasMore ? rows.subList(0, criteria.size()) : rows, criteria.page(), criteria.size(),
                hasMore);
    }

    static GameCard card(ResultSet rs, int i) throws SQLException {
        String tags = rs.getString("top_tags");
        String systems = rs.getString("systems");
        return new GameCard(rs.getLong("id"), rs.getString("title"), rs.getString("slug"), rs.getString("cover_image_url"),
                rs.getObject("release_date", LocalDate.class), rs.getBoolean("windows"), rs.getBoolean("mac"),
                rs.getBoolean("linux"), (Integer) rs.getObject("review_count", Integer.class),
                (Integer) rs.getObject("review_percent_positive", Integer.class),
                tags == null ? List.of() : Arrays.asList(tags.split("\\|")),
                rs.getBoolean("single_player"), rs.getBoolean("co_op"), rs.getBoolean("multiplayer"),
                systems == null ? List.of() : Arrays.stream(systems.split("\\|")).map(GameSystem::valueOf).toList());
    }
}
