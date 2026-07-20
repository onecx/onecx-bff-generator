# onecx-bff-generator
A CLI generator for creating OneCX BFF (Backend-for-Frontend) projects from two OpenAPI specifications:
- **Frontend** API (`/frontend/`)
- **Backend** API (`/backend/`)
The generator produces a standard OneCX project structure with Quarkus configuration, REST controllers, mappers, test scaffolding, Helm charts, and GitHub Actions workflows.
---
## Project Structure
```text
src/main/java/org/tkit/onecx/onecxbffgen/
  commands/        CLI command definitions (Picocli)
  model/           Internal data model
  service/         Core generator logic (analyzer, writer)
  Main.java
src/main/resources/templates/
  bff-project/     Project-level templates (pom.xml, Dockerfile, Helm, etc.)
  entity/          Controller, Mapper, ExceptionMapper templates
  test/            Test class and mockserver templates
```
---
## Building the Generator JAR
```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn -DskipTests clean package -Dquarkus.package.type=uber-jar
```
The output JAR is created at:
```
target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar
```
---
## Running Generator Tests
```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn test
```
---
## Using the Generator

The generator automatically fetches the latest release versions of `onecx-quarkus3-parent`, `docker-quarkus-jvm`, `docker-quarkus-native`, and `helm-quarkus-app` from GitHub at generation time.
### Show help
```bash
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar --help
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar create-bff --help
```
### `create-bff` options
| Option | Description | Default |
|--------|-------------|---------|
| `--name` | Project name (used as artifact ID if `--artifact-id` not set) | required |
| `--group` | Maven `groupId` | `org.tkit.onecx` |
| `--package` | Base Java package (derived from `--group` + name if not set) | derived |
| `--artifact-id` | Maven `artifactId` | same as `--name` |
| `--frontend-api` | Path or URL to the frontend OpenAPI spec | required |
| `--backend-api` | Path or URL to the backend OpenAPI spec | required |
| `--output-dir` | Output directory | current directory |
| `--autobuild` | Run `mvn -B -ntp -DskipTests clean package` after generation | false |
---
## Generating a Project (from any location to any location)
Use the absolute path to the JAR and provide absolute paths for the API specs and output directory:
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name <project-name> \
  --group org.tkit.onecx \
  --package org.tkit.onecx.<domain> \
  --frontend-api /path/to/frontend/openapi-bff.yaml \
  --backend-api /path/to/backend/openapi-svc-internal.yaml \
  --output-dir /path/to/output \
  --autobuild
```
### Example — regenerate `onecx-demo-bff` from local backend path
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name onecx-demo-bff \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --frontend-api /home/Maciej/projects/onecx/onecx-demo-ui/demo/src/assets/api/openapi-bff.yaml \
  --backend-api home/Maciej/projects/onecx/onecx-demo-svc/src/main/openapi/onecx-demo-svc-internal.yaml \
  --output-dir /home/Maciej/projects/onecx \
  --autobuild
```


### Example — regenerate `onecx-demo-bff`
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator-official-repo/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name onecx-demo-bff \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --frontend-api /home/Maciej/projects/onecx/onecx-demo-ui/demo/src/assets/api/openapi-bff.yaml \
  --backend-api https://raw.githubusercontent.com/maciejkryger/onecx-demo-svc/refs/heads/main/src/main/openapi/onecx-demo-svc-internal.yaml \
  --output-dir /home/Maciej/projects/onecx \
  --autobuild
```
raw for develop branch: 
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name onecx-demo-bff \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --frontend-api /home/Maciej/projects/onecx/onecx-demo-ui/demo/src/assets/api/openapi-bff.yaml \
  --backend-api https://raw.githubusercontent.com/maciejkryger/onecx-demo-svc/refs/heads/develop/src/main/openapi/onecx-demo-svc-internal.yaml \
  --output-dir /home/Maciej/projects/onecx \
  --autobuild
```

---
## Generated Project Structure
Given `--group org.tkit.onecx` and `--name onecx-demo-bff`, the base package is normalized to `org.tkit.onecx.demo.bff`.
```text
<output-dir>/<artifact-id>/
  pom.xml
  README.md
  generation-report.json
  .gitignore
  src/main/helm/Chart.yaml
  src/main/helm/values.yaml
  src/main/docker/Dockerfile.jvm
  src/main/docker/Dockerfile.native
  src/main/openapi/frontend/<original-filename>
  src/main/openapi/backend/<original-filename>
  src/main/java/.../rs/controllers/
  src/main/java/.../rs/mappers/
  src/test/java/.../rs/
  src/test/resources/mockserver/
```
---
## Generation Modes
### Standard mode (frontend has paths defined)
Controllers implement the `*ApiService` interfaces generated from the frontend OpenAPI spec.
Mappers translate between:
- Frontend DTOs: `gen.<basePackage>.rs.internal.model.*`
- Backend models: `gen.<basePackage>.backend.client.model.*`
### Fallback mode (frontend has `paths: {}`)
When the frontend OpenAPI defines schemas but no path operations, the generator falls back to using
the backend operations directly. Controllers are generated without a frontend interface, calling the
backend REST client and forwarding the response.
---
## Template Naming Convention
Templates generating Java classes use PascalCase names matching the output class:
- `templates/entity/Controller.java.tpl`
- `templates/entity/Mapper.java.tpl`
- `templates/entity/ExceptionMapper.java.tpl`
- `templates/test/AbstractTest.java.tpl`
- `templates/test/ControllerTest.java.tpl`
- `templates/test/ControllerIT.java.tpl`
Templates generating non-class artifacts use lowercase names:
- `templates/bff-project/pom.xml.tpl`
- `templates/test/mockserver.properties.tpl`
- etc.
