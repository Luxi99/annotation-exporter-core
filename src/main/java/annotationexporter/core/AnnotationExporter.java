package annotationexporter.core;

import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationExporter {

    /*private static final Logger logger = LoggerFactory.getLogger(AnnotationExporter.class);

    public static boolean exportEntry(@NotNull ProjectImageEntry<?> entry, @NotNull ExportDialog.ExportConfig config, @NotNull Project<?> project) throws Exception {

        // Carica l'ImageData dall'entry (legge dal disco)
        qupath.lib.images.servers.ImageServer<?> server;
        PathObjectHierarchy hierarchy;
        try (ImageData<?> imageData = entry.readImageData()) {
            server = imageData.getServer();

            // Aggiorna la gerarchia (padre/figlio tra annotazioni)
            hierarchy = imageData.getHierarchy();
        }
        hierarchy.resolveHierarchy();

        // Raccogli le annotazioni
        List<PathObject> annotations = getAnnotations(config, hierarchy);

        // Salta immagini senza annotazioni
        if (annotations.isEmpty()) {
            logger.info("Nessuna annotazione in '{}', salto.", entry.getImageName());
            return false;
        }

        int w = server.getWidth();
        int h = server.getHeight();

        // Directory di output: <project_dir>/exports/
        Path projectDir = Path.of(project.getPath().getParent().toUri());
        Path outputDir = projectDir.resolve("exports");
        Files.createDirectories(outputDir);

        String baseName = getBaseName(entry.getImageName());
        Path maskPath = outputDir.resolve(baseName + "_mask.tif");
        Path tablePath = outputDir.resolve(baseName + "_table.tsv");

        // Costruisci mask e tabella
        var result = buildMaskAndTable(annotations, w, h, config.separateNuclei());

        // Salva i file
        saveMask((BufferedImage) result.get("image"), "TIFF", maskPath.toString());
        saveTable((List<String>) result.get("table"), "LabelID\tCentroidX\tCentroidY\tClass", tablePath.toString());

        logger.info("Esportata: '{}'", entry.getImageName());
        return true;
    }

    private static @NotNull List<PathObject> getAnnotations(@NotNull ExportDialog.ExportConfig config, @NotNull PathObjectHierarchy hierarchy) {
        List<PathObject> annotations = new ArrayList<>(hierarchy.getAnnotationObjects());

        // Applica il filtro per classe
        filter(config, annotations);
        return annotations;
    }

    private static void filter(ExportDialog.ExportConfig config, List<PathObject> annotations) {
        switch (config.filterMode()) {
            case NONE -> {}
            case EXCLUDE -> annotations.removeIf(a ->
                    config.classNames().contains(
                            a.getPathClass() != null ? a.getPathClass().getName().toLowerCase() : ""
                    )
            );
            case INCLUDE -> annotations.removeIf(a ->
                    !config.classNames().contains(
                            a.getPathClass() != null ? a.getPathClass().getName().toLowerCase() : ""
                    )
            );
        }
    }

    static @NotNull List<PathObject> sortAnnotations(@NotNull List<PathObject> annotations) {
        return annotations.stream()
                .sorted(Comparator.comparingInt(a -> {
                    String name = a.getPathClass() != null
                            ? a.getPathClass().getName().toLowerCase() : "";
                    return name.contains("nucleo") ? 1 : 0;
                }))
                .collect(Collectors.toList());
    }

    static @NotNull Area subtractChildren(@NotNull Shape parentShape, @NotNull List<Shape> childShapes) {
        Area area = new Area(parentShape);
        for (Shape child : childShapes) {
            if (child != null) area.subtract(new Area(child));
        }
        return area;
    }

    static void paintLabel(@NotNull WritableRaster raster, @NotNull Area area, int label, int w, int h) {
        BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = temp.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(area);
        g.dispose();

        WritableRaster binaryRaster = temp.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (binaryRaster.getSample(x, y, 0) > 0) {
                    raster.setSample(x, y, 0, label);
                }
            }
        }
    }

    static @NotNull String createTableRecord(@NotNull PathObject ann, int label) {
        var roi = ann.getROI();
        double cx = roi.getCentroidX();
        double cy = roi.getCentroidY();
        String cls = ann.getPathClass() != null ? ann.getPathClass().getName() : "Unclassified";
        return label + "\t" + cx + "\t" + cy + "\t" + cls;
    }

    static void saveTable(@NotNull List<String> labelTable, @NotNull String header, @NotNull String path) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add(header);
            lines.addAll(labelTable);
            boolean _ = new File(path).getParentFile().mkdirs();
            Files.writeString(Path.of(path), String.join("\n", lines));
            logger.info("Tabella salvata: {}", path);
        } catch (IOException e) {
            logger.error("Errore nel salvataggio della tabella: {}", e.getMessage());
        }
    }

    static void saveMask(@NotNull BufferedImage image, @NotNull String format, @NotNull String path) {
        try {
            boolean _ = new File(path).getParentFile().mkdirs();
            ImageIO.write(image, format, new File(path));
            logger.info("Mask salvata: {}", path);
        } catch (IOException e) {
            logger.error("Errore nel salvataggio della mask: {}", e.getMessage());
        }
    }

    static @NotNull Map<String, Object> buildMaskAndTable(@NotNull List<PathObject> annotations, int w, int h, boolean separateNuclei) {
        BufferedImage labelImage = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = labelImage.getRaster();
        List<String> labelTable = new ArrayList<>();
        int label = 1;

        List<PathObject> sorted = sortAnnotations(annotations);

        if (!separateNuclei) {
            sorted = sorted.stream()
                    .filter(ann -> {
                        String name = ann.getPathClass() != null
                                ? ann.getPathClass().getName().toLowerCase() : "";
                        return !name.contains("nucleo");
                    })
                    .toList();
        }

        for (PathObject ann : sorted) {
            if (label > 65535) {
                logger.warn("Superato il limite di 65535 annotazioni (16 bit). Mi fermo.");
                break;
            }

            var roi = ann.getROI();
            if (roi == null) {
                logger.warn("Annotazione con ROI nulla, salto.");
                continue;
            }
            Shape shape = roi.getShape();
            if (shape == null) continue;

            List<Shape> childShapes = separateNuclei
                    ? ann.getChildObjects().stream()
                    .map(child -> child.getROI() != null ? child.getROI().getShape() : null)
                    .filter(s -> s != null)
                    .collect(Collectors.toList())
                    : List.of();

            Area area = subtractChildren(shape, childShapes);
            paintLabel(raster, area, label, w, h);
            labelTable.add(createTableRecord(ann, label));
            label++;
        }

        return Map.of("image", labelImage, "table", labelTable);
    }

    static @NotNull String getBaseName(@NotNull String imageName) {
        int dot = imageName.indexOf('.');
        return dot > 0 ? imageName.substring(0, dot) : imageName;
    }*/
}