package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record UploadBlobResponse(@Nullable Blob blob) {}
