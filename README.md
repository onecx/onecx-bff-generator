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

Release builds use `gh` with the `CI_PAT` secret to fetch the Maven pre-commit
hook from the latest published release of `onecx/onecx-ai-workflows` before
packaging. The hook is bundled in the JAR, not committed to this repository,
and copied unchanged to `.githooks/pre-commit` at the end of project generation
with executable permissions. The generator initializes a Git repository in the
project directory if needed and enables the hook automatically using
`git config --local core.hooksPath .githooks`. Git must be installed. No hook
download or token is needed at generation time. Local builds without the
downloaded resource print a warning and omit the hook and Git setup.

The hook runs Maven QA checks only when `CI` or `FORCE_CI_COMMIT_HOOKS` is set.
---
## Running Generator Tests
```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn test
```
---
## Using the Generator

The generator automatically fetches the latest release versions of `onecx-quarkus3-parent`, `docker-quarkus-jvm`, `docker-quarkus-native`, and `helm-quarkus-app` from GitHub at generation time.

Without authentication, GitHub allows around 60 API requests per hour. To increase this to 5000 requests/hour, either set `GITHUB_TOKEN` or pass `--github-token`.
### Show help
```bash
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar --help
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar create-bff --help
```
### `create-bff` options
| Option | Description | Default |
|--------|-------------|---------|
| `--name` | Complete project name (used as artifact ID if `--artifact-id` not set) | required |
| `--group` | Maven `groupId` | `org.tkit.onecx` |
| `--package` | Base Java package (derived from `--group` + name if not set) | derived |
| `--artifact-id` | Maven `artifactId` | same as `--name` |
| `--frontend-api` | Path or URL to the frontend OpenAPI spec | required |
| `--backend-api` | Path or URL to the backend OpenAPI spec | required |
| `--output-dir` | Output directory | current directory |
| `--github-token` | Optional GitHub token used to resolve latest dependency versions | env/none |
| `--autobuild` | Run `mvn -B -ntp -DskipTests clean package` after generation | false |

Example with explicit token:

```bash
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar create-bff \
  --name orders-bff \
  --group org.tkit.onecx \
  --frontend-api /path/to/frontend/openapi-bff.yaml \
  --backend-api /path/to/backend/openapi-svc-internal.yaml \
  --github-token ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

`--name` and `--artifact-id` are complete identities; the generator does not add
an organization prefix. For example, `orders-bff` derives the sibling backend
service name `orders-svc`. To reproduce a previous prefixed identity, pass the
prefix explicitly, such as `--name onecx-orders-bff`; custom prefixes such as
`--name acme-orders-bff` are also used as given. The derived backend service
name replaces only the terminal `-bff` suffix, so names without that suffix are
rejected rather than guessed.

`--backend-api` is the OpenAPI input location, not the runtime service URL.
Local backend specs are copied into `openapi/clients/`; remote specs retain
their supplied URL for Maven's build-time download.

If `--package` is provided, it is used as the Java base package as-is. Otherwise
the package is derived from the Maven group and artifact identity; the default
group remains `org.tkit.onecx`.
---
## Generating a Project (from any location to any location)
Use the absolute path to the JAR and provide absolute paths for the API specs and output directory:
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name orders-bff \
  --group org.tkit.onecx \
  --frontend-api /path/to/frontend/openapi-bff.yaml \
  --backend-api /path/to/backend/openapi-svc-internal.yaml \
  --output-dir /path/to/output \
  --autobuild
```
### Example — regenerate `orders-bff` from a local backend path
```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT-runner.jar \
  create-bff \
  --name orders-bff \
  --group org.tkit.onecx \
  --frontend-api /path/to/orders-ui/src/assets/api/openapi-bff.yaml \
  --backend-api /path/to/orders-svc/src/main/openapi/orders-svc-internal.yaml \
  --output-dir /home/Maciej/projects/onecx \
  --autobuild
```


---
## Generated Project Structure
Given `--group org.tkit.onecx` and `--name orders-bff`, the derived base package is `org.tkit.onecx.orders.bff`.
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
  src/main/openapi/<frontend-openapi-filename>
  openapi/clients/<local-backend-openapi-file>
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
