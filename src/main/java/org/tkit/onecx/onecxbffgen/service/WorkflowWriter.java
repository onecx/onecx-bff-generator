package org.tkit.onecx.onecxbffgen.service;

import org.tkit.onecx.onecxbffgen.model.DependencyProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates all .github workflow and configuration files for a BFF project.
 * Based on the onecx/ci-quarkus reusable workflow pattern.
 *
 * Can be reused as-is for backend (svc) generator by calling the same methods
 * with the appropriate projectName / helmEventTargetRepository values.
 */
public class WorkflowWriter {

    /**
     * Writes all .github files: dependabot.yml and all workflow YAMLs.
     *
     * @param projectDir              root directory of the generated project
     * @param projectName             artifact name, e.g. "onecx-demo-bff"
     * @param helmEventTargetRepository GitHub repo for helm release events, e.g. "onecx/onecx-demo"
     *                                  Pass null or blank to omit the helmEventTargetRepository line.
     * @param profile                 dependency profile (used to decide native support)
     */
    public void writeAll(Path projectDir, String projectName, String helmEventTargetRepository, DependencyProfile profile) throws IOException {
        Path workflowsDir = projectDir.resolve(".github/workflows");
        Files.createDirectories(workflowsDir);

        writeDependabot(projectDir);
        writeBuildBranch(workflowsDir);
        writeBuildPrMerge(workflowsDir);
        writeBuildPr(workflowsDir);
        writeBuildRelease(workflowsDir, helmEventTargetRepository);
        writeBuild(workflowsDir, helmEventTargetRepository);
        writeCreateFixBranch(workflowsDir);
        writeCreateNewBuild(workflowsDir);
        writeCreateRelease(workflowsDir);
        writeDocumentation(workflowsDir);
        writeSecurity(workflowsDir);
        writeSonarPr(workflowsDir);
    }

    // -------------------------------------------------------------------------
    // dependabot
    // -------------------------------------------------------------------------
    private void writeDependabot(Path projectDir) throws IOException {
        String content = """
                version: 2
                updates:
                  - package-ecosystem: maven
                    directory: "/"
                    schedule:
                      interval: daily
                    labels:
                      - dependencies
                  - package-ecosystem: "docker"
                    directory: "/src/main/docker"
                    schedule:
                      interval: daily
                    labels:
                      - docker-image
                  - package-ecosystem: "helm"
                    directory: "/src/main/helm"
                    schedule:
                      interval: daily
                    labels:
                      - helm
                """;
        write(projectDir.resolve(".github/dependabot.yml"), content);
    }

    // -------------------------------------------------------------------------
    // build-branch.yml
    // -------------------------------------------------------------------------

    private void writeBuildBranch(Path workflowsDir) throws IOException {
        String content = """
                name: Build Feature Branch

                on:
                  push:
                    branches:
                      - '**'
                      - '!main'
                      - '!fix/[0-9]+.[0-9]+.x'
                      - '![0-9]+.x'
                      - '!dependabot/**'

                jobs:
                  branch:
                    uses: onecx/ci-quarkus/.github/workflows/build-branch.yml@v2
                    secrets: inherit
                    with:
                      native: true
                """;
        write(workflowsDir.resolve("build-branch.yml"), content);
    }

    // -------------------------------------------------------------------------
    // build-pr-merge.yml
    // -------------------------------------------------------------------------

    private void writeBuildPrMerge(Path workflowsDir) throws IOException {
        String content = """
                name: Merge Pull Request

                on:
                  pull_request_target:

                jobs:
                  pr:
                    uses: onecx/ci-quarkus/.github/workflows/build-pr-merge.yml@v2
                    secrets: inherit
                """;
        write(workflowsDir.resolve("build-pr-merge.yml"), content);
    }

    // -------------------------------------------------------------------------
    // build-pr.yml
    // -------------------------------------------------------------------------

    private void writeBuildPr(Path workflowsDir) throws IOException {
        String content = """
                name: Build Pull Request

                on:
                  pull_request:

                jobs:
                  pr:
                    uses: onecx/ci-quarkus/.github/workflows/build-pr.yml@v2
                    secrets: inherit
                    with:
                      native: true
                """;
        write(workflowsDir.resolve("build-pr.yml"), content);
    }

    // -------------------------------------------------------------------------
    // build-release.yml
    // -------------------------------------------------------------------------

    private void writeBuildRelease(Path workflowsDir, String helmEventTargetRepository) throws IOException {
        String helmLine = helmEventTargetRepository != null && !helmEventTargetRepository.isBlank()
                ? "      helmEventTargetRepository: " + helmEventTargetRepository + "\n"
                : "";
        String content = "name: Build Release\n" +
                "on:\n" +
                "  push:\n" +
                "    tags:\n" +
                "      - '**'\n" +
                "jobs:\n" +
                "  release:\n" +
                "    uses: onecx/ci-quarkus/.github/workflows/build-release.yml@v2\n" +
                "    secrets: inherit\n" +
                "    with:\n" +
                "      native: true\n" +
                helmLine;
        write(workflowsDir.resolve("build-release.yml"), content);
    }

    // -------------------------------------------------------------------------
    // build.yml
    // -------------------------------------------------------------------------

    private void writeBuild(Path workflowsDir, String helmEventTargetRepository) throws IOException {
        String helmLine = helmEventTargetRepository != null && !helmEventTargetRepository.isBlank()
                ? "      helmEventTargetRepository: " + helmEventTargetRepository + "\n"
                : "";
        String content = "name: Build\n" +
                "\n" +
                "on:\n" +
                "  workflow_dispatch:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - 'main'\n" +
                "      - 'fix/[0-9]+.[0-9]+.x'\n" +
                "      - '[0-9]+.x'\n" +
                "\n" +
                "jobs:\n" +
                "  build:\n" +
                "    uses: onecx/ci-quarkus/.github/workflows/build.yml@v2\n" +
                "    secrets: inherit\n" +
                "    with:\n" +
                "      native: true\n" +
                helmLine;
        write(workflowsDir.resolve("build.yml"), content);
    }

    // -------------------------------------------------------------------------
    // create-fix-branch.yml
    // -------------------------------------------------------------------------

    private void writeCreateFixBranch(Path workflowsDir) throws IOException {
        String content = """
                name: Create Fix Branch
                on:
                  workflow_dispatch:
                jobs:
                  fix:
                    uses: onecx/ci-common/.github/workflows/create-fix-branch.yml@v1
                    secrets: inherit
                """;
        write(workflowsDir.resolve("create-fix-branch.yml"), content);
    }

    // -------------------------------------------------------------------------
    // create-new-build.yml
    // -------------------------------------------------------------------------

    private void writeCreateNewBuild(Path workflowsDir) throws IOException {
        String content = """
                name: Create new build

                on:
                  workflow_dispatch:

                jobs:
                  build:
                    uses: onecx/ci-common/.github/workflows/create-new-build.yml@v1
                    secrets: inherit
                """;
        write(workflowsDir.resolve("create-new-build.yml"), content);
    }

    // -------------------------------------------------------------------------
    // create-release.yml
    // -------------------------------------------------------------------------

    private void writeCreateRelease(Path workflowsDir) throws IOException {
        String content = """
                name: Create Release Version
                on:
                  workflow_dispatch:
                jobs:
                  release:
                    uses: onecx/ci-common/.github/workflows/create-release.yml@v1
                    secrets: inherit
                """;
        write(workflowsDir.resolve("create-release.yml"), content);
    }

    // -------------------------------------------------------------------------
    // documentation.yml
    // -------------------------------------------------------------------------

    private void writeDocumentation(Path workflowsDir) throws IOException {
        String content = """
                name: Update documentation
                on:
                  push:
                    branches: [ main ]
                    paths:
                      - 'docs/**'
                jobs:
                  release:
                    uses: onecx/ci-common/.github/workflows/documentation.yml@v1
                    secrets: inherit
                """;
        write(workflowsDir.resolve("documentation.yml"), content);
    }

    // -------------------------------------------------------------------------
    // security.yml
    // -------------------------------------------------------------------------

    private void writeSecurity(Path workflowsDir) throws IOException {
        String content = """
                name: Security

                on:
                  schedule:
                    - cron: '0 1 * * 0'
                  workflow_dispatch:

                jobs:
                  security:
                    uses: onecx/ci-common/.github/workflows/security.yml@v1
                    secrets: inherit
                """;
        write(workflowsDir.resolve("security.yml"), content);
    }

    // -------------------------------------------------------------------------
    // sonar-pr.yml
    // -------------------------------------------------------------------------

    private void writeSonarPr(Path workflowsDir) throws IOException {
        String content = """
                name: Sonar Pull Request

                on:
                  workflow_run:
                    workflows: ["Build Pull Request"]
                    types:
                      - completed

                jobs:
                  pr:
                    uses: onecx/ci-quarkus/.github/workflows/quarkus-pr-sonar.yml@v2
                    secrets: inherit
                """;
        write(workflowsDir.resolve("sonar-pr.yml"), content);
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private void write(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
