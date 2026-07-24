package annotationexporter.core;

import org.jetbrains.annotations.NotNull;
import qupath.lib.objects.PathObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExpCore {
    public static @NotNull List<PathObject> sortByHierarchy(@NotNull List<PathObject> annotations) {
        List<PathObject> modifiableList = new ArrayList<>(annotations);
        modifiableList.sort(Comparator.comparingInt(PathObject::getLevel));
        return modifiableList;
    }
}
