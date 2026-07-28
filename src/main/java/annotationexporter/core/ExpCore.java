package annotationexporter.core;

import org.jetbrains.annotations.NotNull;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.interfaces.ROI;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class for exporting annotations from QuPath
 */
public class ExpCore {
    /**
     * Sorts the list of annotations by each annotation's hierarchy level, with annotations
     * deeper in the hierarchy coming last.
     * E.G:
     *
     * Given the list of annotations -> [C, B, E, A, D]
     * And the following hierarchy tree ->
     * A
     * ├─ B
     * │  ├─ C
     * │  ├─ D
     * │
     * E
     *
     * The returned list will be -> [A, E, B, C, D]
     *
     * @param annotations the list of {@code PathObject} annotations to sort
     * @return the sorted list of annotations
     */
    public static @NotNull List<PathObject> sortByHierarchy(@NotNull List<PathObject> annotations) {
        List<PathObject> safeList = new ArrayList<>(annotations);
        safeList.sort(Comparator.comparingInt(PathObject::getLevel));
        return safeList;
    }

    /**
     * Calculates the area of the given annotation, optionally excluding the area of its children (such as nuclei).
     *
     * @param annotation the {@code PathObject} annotation to compute the area for
     * @param excludeChildrenArea if {@code true} the area of the children will be subtracted from the annotation's area (donut shape),
     *                            otherwise the area of the chilren will be filled in the parent object
     * @return the computed {@code Area} object for the annotation
     *
     * @apiNote if the annotation's children have children themselves, they will be ignored and only the main area will be considered
     */
    public static @NotNull Area computeArea(@NotNull PathObject annotation, boolean excludeChildrenArea) {
        Shape shape = annotation.getROI().getShape();
        Area area = new Area(shape);

        if (excludeChildrenArea) {
            for (PathObject child : annotation.getChildObjects()) {
                ROI childRoi = child.getROI();
                if (childRoi != null && childRoi.getShape() != null) {
                    area.subtract(new Area(childRoi.getShape()));
                }
            }
        }

        return area;
    }

    /**
     * Filters the given list of annotations keeping only the elements that match the predicate.
     *
     * @param annotations the {@code List<PathObject>} list of annotations to filter elemets from
     * @param filter the {@code Predicate<PathObject>} to filter elements with
     * @return the filtered {@code List<PathObject>}
     */
    public static @NotNull List<PathObject> filter(@NotNull List<PathObject> annotations, @NotNull Predicate<PathObject> filter) {
        List<PathObject> safeList = new ArrayList<>(annotations);
        return safeList.stream().filter(filter).toList();
    }

    /**
     * Returns a predicate that filters annotations based on the given class names and filter mode.
     *
     * @param mode the {@code FilterMode} to use for filtering
     *             {@link FilterMode#NONE} for no filtering,
     *             {@link FilterMode#EXCLUDE} for excluding the given class names,
     *             {@link FilterMode#INCLUDE} for including only the given class names
     * @param classNames the {@code List<String>} of class names to filter by
     * @return the {@code Predicate<PathObject>} matching the given filter mode and class names
     */
    public static @NotNull Predicate<PathObject> getClassnamesPredicate(@NotNull FilterMode mode, @NotNull List<String> classNames) {
        List<String> safeClassnames = new ArrayList<>(classNames);
        List<String> lowered = safeClassnames.stream().map(String::toLowerCase).toList();

        return switch (mode) {
            case NONE -> p -> true;
            case EXCLUDE -> p -> !lowered.contains(
                    Objects.requireNonNullElse(p.getClassification(), "")
                            .toLowerCase()
            );
            case INCLUDE -> p -> lowered.contains(
                    Objects.requireNonNullElse(p.getClassification(), "")
                            .toLowerCase()
            );
        };

    }

    public static @NotNull LabelResult buildLabelMask(@NotNull List<PathObject> annotations, int IMG_W, int IMG_H, boolean differentiateChildren) {
        return null;
    }

    /**
     * Result of extracting and labeling operations.
     */
    public record LabelResult(@NotNull BufferedImage image, @NotNull List<String> tableRows) {
    }
}
