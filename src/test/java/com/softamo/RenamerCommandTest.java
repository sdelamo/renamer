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
    public void normalizesSpanishAccentsWhitespaceAndCase() {
        assertEquals("cancion_con_espacios.txt", RenamerCommand.normalizeFileName("Canción  con__ESPACIOS.TXT"));
    }
}
