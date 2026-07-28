package annotationexporter.core;

/**
 * Defines the filtering mode used to determine how specific classes are handled during processing.
 *
 * NONE:    Indicates that no filtering is performed. All classes are included without conditions.
 * EXCLUDE: Indicates that the specified class names are excluded from processing.
 * INCLUDE: Indicates that only the specified class names are included in processing, all others are excluded.
 *
 * The {@code FilterMode} is typically used in configurations where class-level filtering
 * is required, such as during export or analysis tasks, to define the behavior for including
 * or excluding specific annotations or objects.
 */
public enum FilterMode {
    NONE,
    EXCLUDE,
    INCLUDE
}
