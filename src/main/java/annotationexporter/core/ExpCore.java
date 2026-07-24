package annotationexporter.core;

import org.jetbrains.annotations.NotNull;
import qupath.lib.objects.PathObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
}
