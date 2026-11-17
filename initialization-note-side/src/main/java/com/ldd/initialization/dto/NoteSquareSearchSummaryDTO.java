package com.ldd.initialization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Search summary request for note square.
 */
@Data
public class NoteSquareSearchSummaryDTO {

    @NotBlank(message = "keyword is required")
    private String keyword;

    private String category;

    @Min(value = 1, message = "maxNotes must be >= 1")
    @Max(value = 20, message = "maxNotes must be <= 20")
    private Integer maxNotes = 10;

    /**
     * recommend | latest | update_time
     */
    private String sortBy = "recommend";

    /**
     * asc | desc
     */
    private String sortOrder = "desc";
}
