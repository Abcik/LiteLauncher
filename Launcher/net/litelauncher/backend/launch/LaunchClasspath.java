package net.litelauncher.backend.launch;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

record LaunchClasspath(List<Entry> entries) {

    LaunchClasspath {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    String text() {
        return entries.stream()
                .map(entry -> entry.path().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    int length() {
        return text().length();
    }

    int libraryCount() {
        int count = 0;
        for (Entry entry : entries) if (entry.role() == Role.LIBRARY) count++;
        return count;
    }

    List<String> clientFileNames() {
        List<String> names = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.role() != Role.CLIENT_RUNTIME) continue;
            Path file = entry.path().getFileName();
            if (file != null) names.add(file.toString());
        }
        return names;
    }

    record Entry(Path path, Role role) {
    }

    enum Role {
        LIBRARY,
        CLIENT_RUNTIME
    }
}
