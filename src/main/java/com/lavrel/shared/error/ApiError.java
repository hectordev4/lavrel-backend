package com.lavrel.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Body of every error response: a stable machine-readable code plus a human-readable message.
 * details lists the invalid fields of a rejected request; it is left out of the JSON when empty.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String error, String message, List<FieldProblem> details) {

    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public ApiError(String error, String message) {
        this(error, message, List.of());
    }

    /** field is the JSON property (or parameter) name, as the client sent it. */
    public record FieldProblem(String field, String problem) {
    }
}
