package annotationexporter.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ExpCoreTest {
    private static PathObject rectangle(double x, double y, double w, double h, String className) {
        ROI roi = ROIs.createRectangleROI(x, y, w, h, ImagePlane.getDefaultPlane());
        return PathObjects.createAnnotationObject(
                roi,
                className == null ? null : PathClass.fromString(className)
        );
    }

    @Test
    void testSortByHierarchyEmptyList() {
        assertThat(ExpCore.sortByHierarchy(List.of())).isEmpty();
    }

    @Test
    void testSortByHierarchy() {
        var parent = rectangle(0, 0, 50, 50, "Nome");
        var child = rectangle(10, 10, 10, 10, "Cognome");

        parent.addChildObject(child);

        List<PathObject> sorted = ExpCore.sortByHierarchy(List.of(child, parent));
        assertThat(sorted).containsExactly(parent, child);
    }
}