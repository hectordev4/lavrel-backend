package com.lavrel.library.adapter.out.persistence;

import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCard;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryPage;
import com.lavrel.library.application.port.out.LibraryQueryPort;
import com.lavrel.shared.domain.Platform;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LibraryJdbcAdapter implements LibraryQueryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public LibraryJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public LibraryPage find(long userId, LibraryCriteria criteria) {
        LibrarySql.Query query = LibrarySql.build(userId, criteria);
        List<LibraryCard> rows = jdbc.query(query.sql(), query.params(), LibraryJdbcAdapter::card);
        boolean hasMore = rows.size() > criteria.size();
        return new LibraryPage(hasMore ? rows.subList(0, criteria.size()) : rows, criteria.page(), criteria.size(),
                hasMore);
    }

    private static LibraryCard card(ResultSet rs, int i) throws SQLException {
        Timestamp lastPlayed = rs.getTimestamp("last_played");
        String ownedOn = rs.getString("owned_on");
        rs.getInt("pin_position");
        boolean pinned = !rs.wasNull();
        return new LibraryCard(rs.getLong("id"), rs.getString("title"), rs.getString("cover_image_url"),
                rs.getObject("release_date", LocalDate.class), rs.getInt("playtime"),
                lastPlayed == null ? null : lastPlayed.toInstant(),
                ownedOn == null ? List.of() : Arrays.stream(ownedOn.split("\\|")).map(Platform::valueOf).toList(),
                rs.getInt("unlocked"), rs.getInt("total"), rs.getString("tracking_status"), pinned);
    }
}
