apiVersion: v2
name: ${artifactId}
version: 0.0.0
appVersion: 0.0.0
description: ${projectName}
keywords:
  - bff
sources:
  - https://github.com/onecx/${artifactId}
maintainers:
  - name: Tkit Developer
    email: tkit_dev@1000kit.org
dependencies:
  - name: helm-quarkus-app
    alias: app
    version: 0.42.0
    repository: oci://ghcr.io/onecx/charts
