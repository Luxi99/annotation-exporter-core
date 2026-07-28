package annotationexporter.core;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.roi.interfaces.ROI;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility class for exporting annotations from QuPath
 */
public class AnnotationExporter {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationExporter.class);
    private static final int MAX_LABEL = 65535;

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

    /**
     * Builds a label mask image and TSV formatted rows from the given annotations.
     * The label mask image is a 16-bit grayscale image
     * with each pixel value (or label) representing a different annotation.
     * The TSV formatted rows are the annotations' data in a tab-separated format:
     * Label, Centroid_X, Centroid_Y, ClassName
     *
     * @apiNote This method works only for already defined hierarchies, if no parent-child relationship is
     * set, there is no guarantee that inner objects will be visible in the final mask. See {@link PathObjectHierarchy#resolveHierarchy()}
     *
     * @param annotations the {@code List<PathObject>} annotations to build the label mask from.
     *                    Must be already sorted or filtered if necessary.
     * @param IMG_W the width of the starting image in pixels.
     * @param IMG_H the height of the starting image in pixels
     * @param differentiateChildren if {@code true} the children of each annotation will be treated as a separate label,
     *                              otherwise they will be ignored.
     * @return the {@code LabelResult} containing the label mask image and the TSV formatted rows.
     */
    public static @NotNull LabelResult buildLabelMask(@NotNull List<PathObject> annotations, int IMG_W, int IMG_H, boolean differentiateChildren) {
        BufferedImage labelImage = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = labelImage.getRaster();
        List<String> tableRows = new ArrayList<>();
        int label = 1;

        List<PathObject> ordered = sortByHierarchy(annotations);

        if (!differentiateChildren) {
            ordered = ordered.stream()
                    .filter(a -> a.getParent() == null || a.getParent().isRootObject())
                    .toList();
        }

        for (PathObject annotation : ordered) {
            if (label > MAX_LABEL) {
                logger.warn("Exceeded max number of annotations (%d, 16 bit image).\nStopping...".formatted(MAX_LABEL));
                break;
            }

            var roi = annotation.getROI();
            if (roi == null || roi.getShape() == null) {
                logger.warn("Undefined ROI.\nskipping...");
                continue;
            }

            Area area = computeArea(annotation, differentiateChildren);

            paintLabel(raster, area, IMG_W, IMG_H, label);
            tableRows.add(toTsvRow(annotation, label));
            label++;
        }

        return new LabelResult(labelImage, tableRows);
    }

    /**
     * Returns a string representation of the given annotation in TSV format. With the following format:
     * Label Centroid_X Centroid_Y ClassName
     *
     * @param annotation the {@code PathObject} annotation to convert to a TSV row
     * @param label the label of the annotation represented as an {@code int}
     * @return the {@code String} as a TSV formatted row with the given annotation's data
     */
    private static String toTsvRow(@NotNull PathObject annotation, int label) {
        var roi = annotation.getROI();
        String className = annotation.getPathClass() != null
                ? annotation.getPathClass().getName()
                : "Unclassified";
        return label + "\t" + roi.getCentroidX() + "\t" + roi.getCentroidY() + "\t" + className;
    }

    /**
     * Paints the given area with the given label in the given raster.
     * @param raster the {@code WritableRaster} to paint the area in
     * @param area the {@code Area} to paint
     * @param IMG_W the width of the final image
     * @param IMG_H the height of the final image
     * @param label the label to paint the area with
     */
    private static void paintLabel(@NotNull WritableRaster raster, @NotNull Area area, int IMG_W, int IMG_H, int label) {
        BufferedImage temp = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = temp.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(area);
        g.dispose();

        WritableRaster binaryRaster = temp.getRaster();
        for (int y = 0; y < IMG_H; y++) {
            for (int x = 0; x < IMG_W; x++) {
                if (binaryRaster.getSample(x, y, 0) > 0) {
                    raster.setSample(x, y, 0, label);
                }
            }
        }
    }

    /**
     * Result of extracting and labeling operations.
     */
    public record LabelResult(@NotNull BufferedImage image, @NotNull List<String> tableRows) {}
}
