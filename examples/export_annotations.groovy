/**
 * Annotation exporter script (Groovy). It's runnable from QuPath's script editor
 * (Run / Run for Project) or headless via:
 *
 *   - The PYTHON WRAPPER included in this folder (suggested method)
 *   - "$QUPATH" script --project "$PROJECT" --args "[SEPARATE_NUCLEI,FILTER_MODE,CLASSNAMES...]" script.groovy
 *
 * QuPath automatically iterates over all the images in the project and runs the script both in
 * headless mode and via the GUI (with "Run for project"), so manually iterating over each image
 * is not needed.
 *
 * Arguments (positional, all optional):
 *   args[0] - differentiateChildren: "true"/"false". Default: true.
 *   args[1] - filterMode: "NONE" | "EXCLUDE" | "INCLUDE". Default: NONE.
 *   args[2..] - name of the classes (case-insensitive) to include/exclude.
 *               Ignored if filterMode is NONE.
 */

import annotationexporter.core.AnnotationExporter
import annotationexporter.core.FilterMode
import annotationexporter.core.AnnotationExporter.LabelResult

import qupath.lib.scripting.QP

import java.nio.file.Path


boolean differantiateChildren = args.length > 0 ? args[0].toBoolean() : true
FilterMode filterMode = args.length > 1 ? FilterMode.valueOf(args[1].toUpperCase()) : FilterMode.NONE
List<String> classNames = args.length > 2 ? args[2..-1] : []

println("[START]")
println("Ann. extraction (differentiateChildren=${differantiateChildren}, filterMode=${filterMode}, classes=${classNames})")
println("========================================================")

def OUTPUT_DIR = QP.buildFilePath(QP.PROJECT_BASE_DIR, "exports")
QP.mkdirs(OUTPUT_DIR)

def imageFileName = QP.getProjectEntry()?.getImageName() ?: "Unnamed.czi"
def imageName = new File(imageFileName).name.replaceFirst(/\.[^.]+$/, "")

Path maskPath = Path.of(QP.buildFilePath(OUTPUT_DIR, imageName + "_mask.tif"))
Path tablePath = Path.of(QP.buildFilePath(OUTPUT_DIR, imageName + "_table.tsv"))

def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
hierarchy.resolveHierarchy()
QP.fireHierarchyUpdate()

def annotations = new ArrayList<>(hierarchy.getAnnotationObjects())

def predicate = AnnotationExporter.getClassnamesPredicate(filterMode, classNames)
def filtered = AnnotationExporter.filter(annotations, predicate)

if (filtered.isEmpty()) {
    println("No annotation found (after filtering)!")
    println("[END]")
    return
}

def server = imageData.getServer()
LabelResult result = AnnotationExporter.buildLabelMask(
        filtered, server.getWidth(), server.getHeight(), differantiateChildren
)

AnnotationExporter.writeImage(result.image(), "TIFF", maskPath)
AnnotationExporter.writeTable(
        result.tableRows(), "LabelID\tCentroidX\tCentroidY\tClass", tablePath
)

println("Mask salvata: ${maskPath}")
println("Tabella salvata: ${tablePath}")
println("[END]")