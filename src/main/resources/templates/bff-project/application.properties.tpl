# AUTHENTICATED
quarkus.http.auth.permission.health.paths=/q/*
quarkus.http.auth.permission.health.policy=permit
quarkus.http.auth.permission.default.paths=/*
quarkus.http.auth.permission.default.policy=authenticated
onecx.permissions.application-id=${quarkus.application.name}
onecx.permissions.product-name=${quarkus.application.name}
org.eclipse.microprofile.rest.client.propagateHeaders=apm-principal-token
# Metadata
onecx.generator.name=${projectName}
onecx.generator.group=${groupId}
onecx.generator.package=${basePackage}
# PROD
%prod.quarkus.rest-client.${backendConfigKey}.url=http://localhost:8080
%prod.quarkus.rest-client.${backendConfigKey}.providers=io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter
%prod.quarkus.oidc-client.client-id=${quarkus.application.name}
# DEV
%dev.quarkus.rest-client.${backendConfigKey}.url=http://localhost:8080
%dev.quarkus.rest-client.onecx_permission.url=${quarkus.mockserver.endpoint}
%dev.quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
%dev.quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
%dev.quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}
%dev.quarkus.mockserver.devservices.config-file=src/test/resources/mockserver.properties
%dev.quarkus.mockserver.devservices.config-dir=src/test/resources/mockserver
%dev.onecx.permissions.enabled=false
%dev.tkit.security.auth.enabled=false
# BUILD
quarkus.openapi-generator.codegen.input-base-dir=target/tmp/openapi
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.config-key=${backendConfigKey}
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.base-package=${backendClientBasePackage}
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.return-response=true
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.additional-api-type-annotations=@org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.additional-model-type-annotations=@io.quarkus.runtime.annotations.RegisterForReflection;
quarkus.openapi-generator.codegen.spec.${backendSpecKey}.enable-security-generation=false
# TEST
quarkus.test.integration-test-profile=test
%test.quarkus.http.test-port=0
%test.tkit.log.json.enabled=false
%test.quarkus.mockserver.devservices.config-class-path=true
%test.quarkus.mockserver.devservices.config-file=/mockserver.properties
%test.quarkus.mockserver.devservices.config-dir=/mockserver
%test.quarkus.mockserver.devservices.log=false
%test.quarkus.mockserver.devservices.reuse=true
%test.quarkus.rest-client.${backendConfigKey}.url=${quarkus.mockserver.endpoint}
%test.quarkus.rest-client.${backendConfigKey}.providers=io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter
%test.tkit.rs.context.token.header-param=apm-principal-token
%test.tkit.rs.context.token.enabled=false
%test.tkit.rs.context.tenant-id.mock.claim-org-id=orgId
%test.quarkus.rest-client.onecx_permission.url=${quarkus.mockserver.endpoint}
%test.quarkus.devservices.timeout=3m
%test.quarkus.oidc.connection-delay=PT30S
%test.quarkus.oidc.connection-timeout=PT10S
%test.quarkus.keycloak.devservices.java-opts=-Dkc.cache=local
%test.quarkus.keycloak.devservices.roles.alice=role-admin
%test.quarkus.keycloak.devservices.roles.bob=role-user
%test.quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
%test.quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
%test.quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}
%test.onecx.permissions.product-name=applications
