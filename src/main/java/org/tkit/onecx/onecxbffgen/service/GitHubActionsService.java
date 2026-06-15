package org.tkit.onecx.onecxbffgen.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubActionsService {

    private static final List<String> WORKFLOW_TEMPLATE_PREFIXES = List.of(
            "templates/github/workflows/",
            "github/workflows/",
            "templates/bff-project/.github/workflows/",
            "bff-project/.github/workflows/"
    );

    private static final List<String> DEPENDABOT_TEMPLATE_CANDIDATES = List.of(
            "templates/github/dependabot.yml.tpl",
            "github/dependabot.yml.tpl",
            "templates/bff-project/.github/dependabot.yml.tpl",
            "bff-project/.github/dependabot.yml.tpl"
    );

    private final TemplateService templates;

    public GitHubActionsService(TemplateService templates) {
        this.templates = templates;
    }

    public void generate(Path projectPath, Map<String, Object> ctx) {
        Map<String, Object> safeCtx = ctx == null ? new HashMap<>() : new HashMap<>(ctx);

        try {
            Path github = projectPath.resolve(".github");
            Path workflows = github.resolve("workflows");

            Files.createDirectories(workflows);

            renderWorkflowIfPresent("build.yml.tpl", workflows, "build.yml", safeCtx);
            renderWorkflowIfPresent("build-branch.yml.tpl", workflows, "build-branch.yml", safeCtx);
            renderWorkflowIfPresent("build-pr.yml.tpl", workflows, "build-pr.yml", safeCtx);
            renderWorkflowIfPresent("build-pr-merge.yml.tpl", workflows, "build-pr-merge.yml", safeCtx);
            renderWorkflowIfPresent("build-release.yml.tpl", workflows, "build-release.yml", safeCtx);

            renderWorkflowIfPresent("create-fix-branch.yml.tpl", workflows, "create-fix-branch.yml", safeCtx);
            renderWorkflowIfPresent("create-new-build.yml.tpl", workflows, "create-new-build.yml", safeCtx);
            renderWorkflowIfPresent("create-release.yml.tpl", workflows, "create-release.yml", safeCtx);

            renderWorkflowIfPresent("documentation.yml.tpl", workflows, "documentation.yml", safeCtx);
            renderWorkflowIfPresent("security.yml.tpl", workflows, "security.yml", safeCtx);
            renderWorkflowIfPresent("sonar-pr.yml.tpl", workflows, "sonar-pr.yml", safeCtx);

            renderDependabotIfPresent(github, safeCtx);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GitHub Actions", e);
        }
    }

    private void renderWorkflowIfPresent(String templateFileName,
                                         Path workflowsDir,
                                         String targetFileName,
                                         Map<String, Object> ctx) {

        String templatePath = findWorkflowTemplate(templateFileName);

        if (templatePath == null) {
            System.err.println("GitHub Actions workflow template not found, skipping: " + templateFileName);
            return;
        }

        templates.renderToFile(
                templatePath,
                workflowsDir.resolve(targetFileName),
                ctx
        );
    }

    private void renderDependabotIfPresent(Path githubDir, Map<String, Object> ctx) {
        String templatePath = findFirstExisting(DEPENDABOT_TEMPLATE_CANDIDATES);

        if (templatePath == null) {
            System.err.println("GitHub dependabot template not found, skipping.");
            return;
        }

        templates.renderToFile(
                templatePath,
                githubDir.resolve("dependabot.yml"),
                ctx
        );
    }

    private String findWorkflowTemplate(String templateFileName) {
        for (String prefix : WORKFLOW_TEMPLATE_PREFIXES) {
            String candidate = prefix + templateFileName;
            if (templateExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String findFirstExisting(List<String> candidates) {
        for (String candidate : candidates) {
            if (templateExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean templateExists(String templateName) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(templateName) != null;
    }
}