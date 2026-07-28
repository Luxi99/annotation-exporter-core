package annotationexporter.core;

import org.jetbrains.annotations.NotNull;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.interfaces.ROI;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

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
        List<PathObject> modifiableList = new ArrayList<>(annotations);
        modifiableList.sort(Comparator.comparingInt(PathObject::getLevel));
        return modifiableList;
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

    public static @NotNull List<PathObject> filter(@NotNull List<PathObject> annotations, @NotNull Predicate<PathObject> filter) {
        return List.of();
    }
}
