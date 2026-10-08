package com.softamo;

import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "renamer", description = "...",
        mixinStandardHelpOptions = true)
public class RenamerCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "...")
    boolean verbose;

    @Parameters(index = "0", arity = "1", paramLabel = "<folder>", description = "Folder whose direct contents will be renamed")
    Path folder;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(RenamerCommand.class, args);
    }

    public void run() {
        if (!Files.isDirectory(folder)) {
            throw new CommandLine.ParameterException(new CommandLine(this), folder + " is not a folder");
        }

        try (Stream<Path> paths = Files.list(folder)) {
            List<Path> contents = paths.toList();
            Map<Path, Path> renames = contents.stream()
                .collect(Collectors.toMap(path -> path, path -> path.resolveSibling(normalizeFileName(path.getFileName().toString()))));
            renames.entrySet().removeIf(rename -> rename.getKey().equals(rename.getValue()));

            ensureNoCollisions(contents, renames);

            for (Map.Entry<Path, Path> rename : renames.entrySet()) {
                move(rename.getKey(), rename.getValue());
                if (verbose) {
                    System.out.println(rename.getKey().getFileName() + " -> " + rename.getValue().getFileName());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to rename contents of " + folder, e);
        }
    }

    static String normalizeFileName(String fileName) {
        String withoutAccents = Normalizer.normalize(fileName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return withoutAccents
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_")
            .toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Moves {@code source} to {@code target}. On case-insensitive file systems (such as the macOS default)
     * {@link Files#move} treats a case-only rename as a no-op, so those go through a temporary name.
     */
    private static void move(Path source, Path target) throws IOException {
        if (isSameFile(source, target)) {
            Path temporary = Files.createTempFile(source.getParent(), ".renamer-", ".tmp");
            Files.delete(temporary);
            Files.move(source, temporary);
            Files.move(temporary, target);
        } else {
            Files.move(source, target);
        }
    }

    private static boolean isSameFile(Path a, Path b) throws IOException {
        return Files.exists(b) && Files.isSameFile(a, b);
    }

    private static void ensureNoCollisions(List<Path> contents, Map<Path, Path> renames) throws IOException {
        Map<Path, List<Path>> sourcesByTarget = renames.entrySet().stream()
            .collect(Collectors.groupingBy(Map.Entry::getValue,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        for (Map.Entry<Path, List<Path>> entry : sourcesByTarget.entrySet()) {
            Path target = entry.getKey();
            List<Path> sources = entry.getValue();
            if (sources.size() > 1 || (Files.exists(target) && !isSameFile(sources.get(0), target))) {
                throw new IllegalStateException("Cannot rename files because " + target.getFileName() + " would collide with an existing name");
            }
        }
    }
}
