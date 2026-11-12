package org.tkit.onecx.onecxbffgen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectWriterTest {
    @TempDir
    Path projectDir;

    @Test
    void shouldCopyBundledHookUnchangedAndMakeItExecutable() throws Exception {
        byte[] content = "#!/bin/sh\nprintf '%s' \"${CI:-}\"\n".getBytes(StandardCharsets.UTF_8);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(hookClassLoader(content, original));
        try {
            new ProjectWriter().writePreCommitHook(projectDir);

            Path output = projectDir.resolve(".githooks/pre-commit");
            assertArrayEquals(content, Files.readAllBytes(output));
            assertTrue(Files.isExecutable(output));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void shouldEnableHookForCommitsWithBehaviorControlledByEnvironment() throws Exception {
        byte[] content = "#!/bin/sh\n[ \"${BFF_GENERATOR_TEST_REJECT_COMMIT:-}\" != \"1\" ]\n"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(hookClassLoader(content, original));
        try {
            new ProjectWriter().writePreCommitHook(projectDir);

            ProcessBuilder commit = git(projectDir, "-c", "user.name=Generator Test",
                    "-c", "user.email=generator-test@example.invalid", "-c", "commit.gpgsign=false",
                    "commit", "--quiet", "--allow-empty",
                    "-m", "test: verify default hook activation",
                    "-m", "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>");
            commit.environment().put("BFF_GENERATOR_TEST_REJECT_COMMIT", "1");
            assertEquals(1, commit.start().waitFor(), "The enabled hook must reject the commit");
            commit.environment().remove("BFF_GENERATOR_TEST_REJECT_COMMIT");
            assertEquals(0, commit.start().waitFor(), "The hook must allow commits when the env var is unset");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void shouldNotChangeParentRepositoryWhenGeneratingNestedProject() throws Exception {
        assertEquals(0, git(projectDir, "init", "--quiet").start().waitFor());
        assertEquals(0, git(projectDir, "config", "--local", "core.hooksPath", "parent-hooks").start().waitFor());
        Path nestedProject = Files.createDirectory(projectDir.resolve("generated-bff"));
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(hookClassLoader("#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), original));
        try {
            ProjectWriter writer = new ProjectWriter();
            writer.writePreCommitHook(nestedProject);
            assertTrue(Files.isDirectory(nestedProject.resolve(".git")));
            writer.writePreCommitHook(nestedProject);

            Process config = git(projectDir, "config", "--local", "--get", "core.hooksPath")
                    .redirectOutput(ProcessBuilder.Redirect.PIPE).start();
            assertEquals("parent-hooks", new String(config.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());
            assertEquals(0, config.waitFor());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void shouldWarnAndLeaveExistingHookUnchangedWhenResourceIsNotBundled() throws Exception {
        Path output = projectDir.resolve(".githooks/pre-commit");
        Files.createDirectories(output.getParent());
        byte[] existing = "existing hook\n".getBytes(StandardCharsets.UTF_8);
        Files.write(output, existing);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        Thread.currentThread().setContextClassLoader(hookClassLoader(null, original));
        try (PrintStream captured = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(captured);
            new ProjectWriter().writePreCommitHook(projectDir);

            assertArrayEquals(existing, Files.readAllBytes(output));
            assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Pre-commit hook not bundled"));
            assertFalse(Files.exists(projectDir.resolve(".git")));
        } finally {
            System.setErr(originalErr);
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private ClassLoader hookClassLoader(byte[] content, ClassLoader parent) {
        return new ClassLoader(parent) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if ("hooks/pre-commit".equals(name)) {
                    return content == null ? null : new ByteArrayInputStream(content);
                }
                return super.getResourceAsStream(name);
            }
        };
    }

    private ProcessBuilder git(Path directory, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        return new ProcessBuilder(command).directory(directory.toFile()).inheritIO();
    }
}
