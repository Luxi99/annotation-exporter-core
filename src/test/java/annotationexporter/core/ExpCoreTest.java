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

import java.awt.geom.Area;
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

    @Test
    void testSortByHierarchyMultipleNested() {
        PathObject grandparent = rectangle(0, 0, 100, 100, "A");
        PathObject parent = rectangle(10, 10, 50, 50, "B");
        PathObject child1 = rectangle(20, 20, 10, 10, "C");
        PathObject child2 = rectangle(30, 20, 5, 5, "D");

        grandparent.addChildObject(parent);
        parent.addChildObject(child1);
        parent.addChildObject(child2);

        List<PathObject> sorted = ExpCore.sortByHierarchy(List.of(parent, child1, grandparent, child2));

        assertThat(sorted).containsExactly(grandparent, parent, child1, child2);
    }

    @Test
    void computeAreaIncludeChildrenEmptyShape() {
        PathObject parent = rectangle(0, 0, 0, 0, "Cellula");
        PathObject child = rectangle(0, 0, 0, 0, "Nucleo");
        parent.addChildObject(child);

        Area area = ExpCore.computeArea(parent, false);

        assertThat(area.getBounds()).isEqualTo(new Area().getBounds());
        assertThat(area.isEmpty()).isTrue();
    }

    @Test
    void computeAreaIncludeChildren() {
        PathObject parent = rectangle(0, 0, 40, 40, "Cellula");
        PathObject child = rectangle(10, 10, 10, 10, "Nucleo");
        parent.addChildObject(child);

        Area area = ExpCore.computeArea(parent, false);

        assertThat(area.contains(15, 15)).isTrue();
    }
}