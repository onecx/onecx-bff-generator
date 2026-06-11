package org.tkit.onecx.onecxbffgen.model;
public record OperationModel(String operationId, String httpMethod, String path,
                             String requestBodyType, String responseType, int successStatusCode,
                             String resolvedBackendOperationId, String resolvedBackendRequestBodyType,
                             String resolvedBackendResponseType, String resolvedBackendPath) {
    public OperationModel(String operationId, String httpMethod, String path,
                          String requestBodyType, String responseType) {
        this(operationId, httpMethod, path, requestBodyType, responseType, 0, null, null, null, null);
    }
    public OperationModel(String operationId, String httpMethod, String path,
                          String requestBodyType, String responseType, int successStatusCode) {
        this(operationId, httpMethod, path, requestBodyType, responseType, successStatusCode, null, null, null, null);
    }
    public boolean hasRequestBody() {
        return requestBodyType != null && !requestBodyType.isBlank();
    }
    public boolean hasResponseBody() {
        return responseType != null && !responseType.isBlank();
    }
}
