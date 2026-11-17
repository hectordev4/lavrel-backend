package com.lavrel.library.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.library.domain.CollectionNameTakenException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/** How the adapter reads an integrity error, without a database (CollectionJdbcAdapterTest covers the real one). */
class CollectionNameClashTest {

    @Test
    void aClashOnTheUniqueNameIndexIsANameTaken() {
        DuplicateKeyException clash = new DuplicateKeyException("PreparedStatementCallback",
                new SQLException("ERROR: duplicate key value violates unique constraint \"uq_collection_user_name\"",
                        "23505"));

        assertThat(CollectionJdbcAdapter.nameTakenOrRethrow(clash, "Cozy"))
                .isInstanceOf(CollectionNameTakenException.class)
                .hasMessageContaining("Cozy");
    }

    @Test
    void anyOtherIntegrityErrorIsReturnedUnchanged() {
        DataIntegrityViolationException other = new DataIntegrityViolationException("PreparedStatementCallback",
                new SQLException("ERROR: insert or update on table \"collection\" violates foreign key constraint "
                        + "\"collection_user_id_fkey\"", "23503"));

        assertThat(CollectionJdbcAdapter.nameTakenOrRethrow(other, "Cozy")).isSameAs(other);
    }
}
