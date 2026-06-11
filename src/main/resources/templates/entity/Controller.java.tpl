package ${packageName};

${apiServiceImportStatement}
${frontendModelImportStatement}
${backendModelImportStatement}
import ${backendClientImport};
import ${exceptionMapperImport};
import ${mapperImport};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
@LogService
${classPathAnnotation}
public class ${className}${apiServiceTypeSuffix} {

    @Inject
    @RestClient
    ${backendClientType} client;

    @Inject
    ${mapperType} mapper;

    @Inject
    ExceptionMapper exceptionMapper;

${methods}
    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }
}





