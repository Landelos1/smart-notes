package com.ldd.initialization.dto;

import lombok.Data;

/**
 * Search params for note square.
 */
@Data
public class NoteSquareSearchDTO {

    private String keyword;

    /**
     * recommend | latest | update_time
     */
    private String sortBy = "recommend";

    /**
     * asc | desc
     */
    private String sortOrder = "desc";

    private Integer page = 1;

    private Integer size = 10;
}
