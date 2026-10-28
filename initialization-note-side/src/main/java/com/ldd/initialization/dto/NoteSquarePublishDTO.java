package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Publish note to note square.
 */
@Data
public class NoteSquarePublishDTO {

    @NotNull(message = "noteId is required")
    private Long noteId;

    @Size(max = 500, message = "summary length must be <= 500")
    private String summary;

    @Size(max = 255, message = "coverUrl length must be <= 255")
    private String coverUrl;
}
