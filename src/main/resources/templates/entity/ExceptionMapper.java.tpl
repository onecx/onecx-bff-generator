package ${packageName};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
@ApplicationScoped
public class ExceptionMapper {
    public RestResponse<String> constraint(ConstraintViolationException ex) {
        return RestResponse.status(Response.Status.BAD_REQUEST, ex.getMessage());
    }
    public Response clientException(ClientWebApplicationException ex) {
        return Response.status(ex.getResponse().getStatus())
                .entity(ex.getMessage())
                .build();
    }
}
