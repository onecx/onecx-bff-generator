# onecx-bff-generator

Generator CLI do tworzenia projektów OneCX BFF na bazie dwóch specyfikacji OpenAPI:

- frontowej (`/frontend/`)
- backendowej (`/backend/`)

Generator domyślnie tworzy projekt w standardzie OneCX (struktura `domain`/`rs`, workflowy, test scaffold, konfiguracja Quarkus/OneCX).

## Wzorzec

Generator jest porządkowany funkcjonalnie na wzór generatora backendowego pod ścieżką:

`https://github.com/maciejkryger/onecx-svc-generator`

## Struktura projektu generatora

```text
src/main/java/org/tkit/onecx/onecxbffgen/
  commands/
  model/
  service/
  Main.java

src/main/resources/templates/
  bff-project/
  entity/
  test/
```

## Nazewnictwo templatek

Template, który generuje klasę Java zaczynającą się wielką literą, też ma nazwę zaczynającą się wielką literą:

- `templates/entity/Controller.java.tpl`
- `templates/entity/Mapper.java.tpl`
- `templates/entity/ExceptionMapper.java.tpl`
- `templates/test/AbstractTest.java.tpl`
- `templates/test/ControllerTest.java.tpl`
- `templates/test/ControllerIT.java.tpl`

Pliki generujące artefakty nie-klasowe zostają lowercase (np. `pom.xml.tpl`, `README.md.tpl`, `workflow.yml.tpl`, `mockserver.properties.tpl`).

## CLI (subcommand jak w svc-generator)

Root command uruchamia subcommand `create-bff`:

```bash
onecx-bff-generator create-bff ...
```

### Opcje `create-bff`

- `--name` / `--project-name` - nazwa projektu BFF
- `--group` / `--group-id` - Maven `groupId` (domyślnie `org.tkit.onecx`)
- `--package` - bazowy pakiet Java (jeśli brak, wyliczany z `group` + `artifact-id`)
- `--artifact-id` - opcjonalny Maven `artifactId`
- `--frontend-api` - ścieżka lokalna lub URL do OpenAPI frontu
- `--backend-api` - ścieżka lokalna lub URL do OpenAPI backendu
- `--output-dir` - katalog wyjściowy (domyślnie bieżący)
- `--autobuild` - po wygenerowaniu uruchamia `mvn -B -ntp -DskipTests clean package`
- `--parent-version` - wersja `onecx-quarkus3-parent` (jeśli brak, pobierane latest release)

## Logika parent version

Jeżeli nie podasz `--parent-version`, generator próbuje pobrać latest release z:

`https://github.com/onecx/onecx-quarkus3-parent/releases`

### Dla parent `>= 3.1.0`

- Java `25`
- `<packaging>quarkus</packaging>`
- `quarkus-junit`
- `quarkus-junit-mockito`

### Dla parent `<= 2.5.0`

- Java `17`
- `quarkus-junit5`
- `quarkus-junit5-mockito`
- testowy `swagger-parser`

### Dla parent `2.5.1 - 3.0.x`

- Java `17`
- `quarkus-junit`
- `quarkus-junit-mockito`
- bez testowego `swagger-parser`

## Jak uruchomić generator

```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn -q -DskipTests exec:java -Dexec.args="--help"
```

Help subcommand:

```bash
mvn -q -DskipTests exec:java -Dexec.args="create-bff --help"
```

## Kompilacja generatora

```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn -q -DskipTests clean package
```

## Uruchomienie z jara

```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT.jar --help
```

```bash
java -jar target/onecx-bff-generator-1.0.0-SNAPSHOT.jar create-bff --help
```

Przykład generacji `onecx-demo-bff`:

```bash
java -jar /home/Maciej/projects/onecx/onecx-bff-generator/target/onecx-bff-generator-1.0.0-SNAPSHOT.jar create-bff \
  --name onecx-demo-bff \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --frontend-api /home/Maciej/projects/onecx/onecx-demo-ui/demo/src/assets/api/openapi-bff.yaml \
  --backend-api /home/Maciej/projects/onecx/onecx-demo-svc/src/main/openapi/onecx-demo-svc-internal.yaml \
  --output-dir /home/Maciej/projects/onecx \
  --autobuild
```

## Co generator zapisuje

Przy `--group org.tkit.onecx` i `--artifact-id onecx-demo-bff` bazowy pakiet jest normalizowany do `org.tkit.onecx.demo.bff`.

```text
<output-dir>/<artifact-id>/
  pom.xml
  README.md
  generation-report.json
  <artifact-id>.adoc
  src/main/helm/Chart.yaml
  src/main/helm/values.yaml
  src/main/docker/Dockerfile.jvm
  src/main/docker/Dockerfile.native
  src/main/openapi/frontend/<oryginalna-nazwa-pliku>
  src/main/openapi/backend/<oryginalna-nazwa-pliku>
  src/main/java/.../rs/controllers/
  src/main/java/.../rs/mappers/
  src/test/java/.../rs/
  src/test/resources/mockserver/
  .github/workflows/*.yml
```

## Implementacja `*ApiService`

Kontrolery są generowane jako klasy `*RestController`, które implementują interfejsy `*ApiService` wygenerowane z frontend OpenAPI (pakiet `gen.<basePackage>.rs.internal`).

Mapery używają modeli generowanych z OpenAPI:
- frontend: `gen.<basePackage>.rs.internal.model.*DTO`
- backend: `gen.<basePackage>.backend.client.model.*`

## Testy generatora

```bash
cd /home/Maciej/projects/onecx/onecx-bff-generator
mvn -q test
```
