package annotationexporter.core;

import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

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
        var a = rectangle(5, 5, 20, 20, "red blood cell");
        var b = rectangle(40, 40, 20, 20, "lymphocyte");

        var result = AnnotationExporter.buildLabelMask(List.of(a, b), W, H, true);
        var raster = result.image().getRaster();

        assertThat(raster.getSample(15, 15, 0)).isEqualTo(1);
        assertThat(raster.getSample(50, 50, 0)).isEqualTo(2);
        assertThat(raster.getSample(0, 0, 0)).isEqualTo(0);
    }

    @Test
    void testDifferentiateChildrenFromParent() {
        var child = rectangle(25, 25, 10, 10, "nucleo");
        var parent = rectangle(10, 10, 50, 50, "cellula cancerosa");
        parent.addChildObject(child);

        var result = AnnotationExporter.buildLabelMask(List.of(parent, child), W, H, true);

        assertThat(result.tableRows()).hasSize(2);
        assertThat(result.image().getRaster().getSample(30, 30, 0)).isNotEqualTo(1);
    }
}
