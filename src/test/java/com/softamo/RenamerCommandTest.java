package com.softamo;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RenamerCommandTest {

    @Test
    public void renamesFolderContents(@TempDir Path folder) throws Exception {
        Files.createFile(folder.resolve("Canción  con__ESPACIOS.TXT"));
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[] { folder.toString() };
            PicocliRunner.run(RenamerCommand.class, ctx, args);

            assertTrue(Files.exists(folder.resolve("cancion_con_espacios.txt")));
        }
    }

    @Test
    public void skipsFilesWhoseNameIsAlreadyNormalized(@TempDir Path folder) throws Exception {
        Path alreadyNormalized = folder.resolve("already_normalized.txt");
        Files.createFile(alreadyNormalized);
        Files.createFile(folder.resolve("Needs Rename.TXT"));
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[] { folder.toString() };
            PicocliRunner.run(RenamerCommand.class, ctx, args);

            assertTrue(Files.exists(alreadyNormalized));
            assertTrue(Files.exists(folder.resolve("needs_rename.txt")));
            try (var paths = Files.list(folder)) {
                assertEquals(2, paths.count());
            }
        }
    }

    @Test
    public void renamesWhenOnlyTheCaseChanges(@TempDir Path folder) throws Exception {
        Files.createFile(folder.resolve("7.Obuhovski-Grabszewski.mp4"));
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[] { folder.toString() };
            PicocliRunner.run(RenamerCommand.class, ctx, args);

            try (var paths = Files.list(folder)) {
                assertEquals(java.util.List.of("7.obuhovski-grabszewski.mp4"),
                    paths.map(p -> p.getFileName().toString()).toList());
            }
        }
    }

    @Test
    public void normalizesSpanishAccentsWhitespaceAndCase() {
        assertEquals("cancion_con_espacios.txt", RenamerCommand.normalizeFileName("Canción  con__ESPACIOS.TXT"));
    }
}
