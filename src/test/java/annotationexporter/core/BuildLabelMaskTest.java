package annotationexporter.core;

import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.image.WritableRaster;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildLabelMaskTest {
    private static final int W = 100;
    private static final int H = 100;

    private static PathObject rectangle(double x, double y, double w, double h, String className) {
        ROI roi = ROIs.createRectangleROI(x, y, w, h, ImagePlane.getDefaultPlane());
        return PathObjects.createAnnotationObject(
                roi,
                className == null ? null : PathClass.fromString(className)
        );
    }

    @Test
    void testBuildLabelMaskCorrectDimensions() {
        PathObject parent = rectangle(0, 0, 40, 40, "Cellula");
        PathObject child = rectangle(10, 10, 10, 10, "Nucleo");
        parent.addChildObject(child);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(
                List.of(child, parent), 50, 50, true
        );

        assertThat(result.tableRows()).hasSize(2);
        assertThat(result.image().getWidth()).isEqualTo(50);
    }

    @Test
    void testEmptyListReturnsEmptyImageAndTable() {
        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(List.of(), W, H, true);

        assertThat(result.tableRows()).isEmpty();
        assertThat(result.image().getRaster().getSample(50, 50, 0)).isEqualTo(0);
    }

    @Test
    void labelsAreSequentialStartingFromOne() {
        PathObject a = rectangle(5, 5, 20, 20, "red blood cell");
        PathObject b = rectangle(40, 40, 20, 20, "lymphocyte");

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(List.of(a, b), W, H, true);
        WritableRaster raster = result.image().getRaster();

        assertThat(raster.getSample(15, 15, 0)).isEqualTo(1);
        assertThat(raster.getSample(50, 50, 0)).isEqualTo(2);
        assertThat(raster.getSample(0, 0, 0)).isEqualTo(0);
    }

    @Test
    void testDifferentiateChildrenFromParent() {
        PathObject child = rectangle(25, 25, 10, 10, "nucleo");
        PathObject parent = rectangle(10, 10, 50, 50, "cellula cancerosa");
        parent.addChildObject(child);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(List.of(parent, child), W, H, true);

        assertThat(result.tableRows()).hasSize(2);
        assertThat(result.image().getRaster().getSample(30, 30, 0)).isNotEqualTo(1);
    }

    @Test
    void testDifferentiateChildrenFromParentNoMore() {
        PathObject child = rectangle(25, 25, 10, 10, "nucleo");
        PathObject parent = rectangle(10, 10, 50, 50, "cellula cancerosa");
        parent.addChildObject(child);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(List.of(parent, child), W, H, false);

        assertThat(result.tableRows()).hasSize(1);
        assertThat(result.image().getRaster().getSample(30, 30, 0)).isEqualTo(1);
    }

    @Test
    void testDeeplyNestedAnnotationsTSV() {
        PathObject grandchild = rectangle(30, 30, 5, 5, "livello3");
        PathObject child = rectangle(20, 20, 20, 20, "livello2");
        PathObject parent = rectangle(10, 10, 50, 50, "livello1");
        child.addChildObject(grandchild);
        parent.addChildObject(child);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(
                List.of(grandchild, parent, child), W, H, true
        );

        assertThat(result.tableRows()).hasSize(3);
        assertThat(result.tableRows().get(0)).contains("livello1");
        assertThat(result.tableRows().get(1)).contains("livello2");
        assertThat(result.tableRows().get(2)).contains("livello3");
    }

    @Test
    void testDeeplyNestedAnnotationsLabels() {
        PathObject grandchild = rectangle(30, 30, 5, 5, "livello3");
        PathObject child = rectangle(20, 20, 20, 20, "livello2");
        PathObject parent = rectangle(10, 10, 50, 50, "livello1");
        child.addChildObject(grandchild);
        parent.addChildObject(child);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(
                List.of(grandchild, parent, child), W, H, true
        );

        assertThat(result.image().getRaster().getSample(0, 0, 0)).isEqualTo(0);
        assertThat(result.image().getRaster().getSample(10, 10, 0)).isEqualTo(1);
        assertThat(result.image().getRaster().getSample(20, 20, 0)).isEqualTo(2);
        assertThat(result.image().getRaster().getSample(30, 30, 0)).isEqualTo(3);
    }

    @Test
    void testUnclassifiedAnnotation() {
        PathObject noClass = rectangle(0, 0, 20, 20, null);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(List.of(noClass), W, H, true);

        assertThat(result.tableRows()).hasSize(1);
        assertThat(result.tableRows().getFirst()).endsWith("Unclassified");
    }

    @Test
    void testMoreThanOneChildMultipleHoles() {
        PathObject child1 = rectangle(15, 15, 8, 8, "nucleo1");
        PathObject child2 = rectangle(40, 40, 8, 8, "nucleo2");
        PathObject parent = rectangle(10, 10, 60, 60, "cellula");
        parent.addChildObject(child1);
        parent.addChildObject(child2);

        AnnotationExporter.LabelResult result = AnnotationExporter.buildLabelMask(
                List.of(parent, child1, child2), W, H, true
        );
        WritableRaster raster = result.image().getRaster();

        assertThat(result.tableRows()).hasSize(3);
        assertThat(raster.getSample(19, 19, 0)).isNotEqualTo(1);
        assertThat(raster.getSample(44, 44, 0)).isNotEqualTo(1);
        assertThat(raster.getSample(12, 12, 0)).isEqualTo(1);
        assertThat(raster.getSample(19, 19, 0)).isNotEqualTo(raster.getSample(44, 44, 0));
    }
}
