package annotationexporter.core;

import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.geom.Area;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationExporterTest {
    private static PathObject rectangle(double x, double y, double w, double h, String className) {
        ROI roi = ROIs.createRectangleROI(x, y, w, h, ImagePlane.getDefaultPlane());
        return PathObjects.createAnnotationObject(
                roi,
                className == null ? null : PathClass.fromString(className)
        );
    }

    @Test
    void testSortByHierarchyEmptyList() {
        assertThat(AnnotationExporter.sortByHierarchy(List.of())).isEmpty();
    }

    @Test
    void testSortByHierarchy() {
        var parent = rectangle(0, 0, 50, 50, "Nome");
        var child = rectangle(10, 10, 10, 10, "Cognome");

        parent.addChildObject(child);

        List<PathObject> sorted = AnnotationExporter.sortByHierarchy(List.of(child, parent));
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

        List<PathObject> sorted = AnnotationExporter.sortByHierarchy(List.of(parent, child1, grandparent, child2));

        assertThat(sorted).containsExactly(grandparent, parent, child1, child2);
    }

    @Test
    void computeAreaIncludeChildrenEmptyShape() {
        PathObject parent = rectangle(0, 0, 0, 0, "Cellula");
        PathObject child = rectangle(0, 0, 0, 0, "Nucleo");
        parent.addChildObject(child);

        Area area = AnnotationExporter.computeArea(parent, false);

        assertThat(area.getBounds()).isEqualTo(new Area().getBounds());
        assertThat(area.isEmpty()).isTrue();
    }

    @Test
    void computeAreaIncludeChildren() {
        PathObject parent = rectangle(0, 0, 40, 40, "Cellula");
        PathObject child = rectangle(10, 10, 10, 10, "Nucleo");
        parent.addChildObject(child);

        Area area = AnnotationExporter.computeArea(parent, false);

        assertThat(area.contains(15, 15)).isTrue();
    }

    @Test
    void computeAreaExcludeChildrenEmptyShape() {
        PathObject parent = rectangle(0, 0, 0, 0, "Cellula");
        PathObject child = rectangle(0, 0, 0, 0, "Nucleo");
        parent.addChildObject(child);

        Area area = AnnotationExporter.computeArea(parent, true);

        assertThat(area.getBounds()).isEqualTo(new Area().getBounds());
        assertThat(area.isEmpty()).isTrue();
    }

    @Test
    void computeAreaExcludeChild() {
        var parent = rectangle(0, 0, 40, 40, "Cellula");
        var child = rectangle(10, 10, 10, 10, "Nucleo");
        parent.addChildObject(child);

        var area = AnnotationExporter.computeArea(parent, true);

        assertThat(area.contains(5, 5)).isTrue();
        assertThat(area.contains(15, 15)).isFalse();
    }

    @Test
    void computeAreaExcludeMoreChildren() {
        var parent = rectangle(0, 0, 40, 40, "Cellula");
        var child1 = rectangle(10, 10, 10, 10, "N1");
        var child2 = rectangle(20, 10, 5, 5, "N2");
        parent.addChildObject(child1);
        parent.addChildObject(child2);

        var area = AnnotationExporter.computeArea(parent, true);

        assertThat(area.contains(5, 5)).isTrue();
        assertThat(area.contains(25, 16)).isTrue();
        assertThat(area.contains(15, 15)).isFalse();
        assertThat(area.contains(23, 13)).isFalse();
    }

    @Test
    void filterWithArbitraryPredicateKeepsMatchingOnly() {
        PathObject a = rectangle(0, 0, 10, 10, "Tumore");
        PathObject b = rectangle(0, 0, 10, 10, "Stroma");
        PathObject c = rectangle(0, 0, 0, 0, "Tumore");

        List<PathObject> result = AnnotationExporter.filter(
                List.of(a, b, c),
                p -> p.getROI().getArea() > 0 && "Tumore".equals(p.getPathClass().getName())
        );

        assertThat(result).containsExactlyInAnyOrder(a);
    }

    @Test
    void testClassNamePredicateExcludeMode() {
        Predicate<PathObject> predicate = AnnotationExporter.getClassnamesPredicate(
                FilterMode.EXCLUDE, List.of("artefatto")
        );
        PathObject artefatto = rectangle(0, 0, 10, 10, "Artefatto");
        PathObject manufatto = rectangle(0, 0, 10, 10, "Manufatto");

        assertThat(predicate.test(artefatto)).isFalse();
        assertThat(predicate.test(manufatto)).isTrue();
    }

    @Test
    void testClassNamePredicateIncludeMode() {
        Predicate<PathObject> predicate = AnnotationExporter.getClassnamesPredicate(
                FilterMode.INCLUDE, List.of("artefatto")
        );
        PathObject artefatto = rectangle(0, 0, 10, 10, "Artefatto");
        PathObject manufatto = rectangle(0, 0, 10, 10, "Manufatto");

        assertThat(predicate.test(artefatto)).isTrue();
        assertThat(predicate.test(manufatto)).isFalse();
    }

    @Test
    void testClassNamePredicateNoneMode() {
        Predicate<PathObject> predicate = AnnotationExporter.getClassnamesPredicate(
                FilterMode.NONE, List.of("artefatto")
        );
        PathObject artefatto = rectangle(0, 0, 10, 10, "Artefatto");
        PathObject manufatto = rectangle(0, 0, 10, 10, "Manufatto");

        assertThat(predicate.test(artefatto)).isTrue();
        assertThat(predicate.test(manufatto)).isTrue();

    }
}