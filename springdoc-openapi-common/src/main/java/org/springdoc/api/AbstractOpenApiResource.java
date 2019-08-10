package org.springdoc.api;

import java.util.Set;

import org.springdoc.core.AbstractRequestBuilder;
import org.springdoc.core.AbstractResponseBuilder;
import org.springdoc.core.InfoBuilder;
import org.springdoc.core.MediaAttributes;
import org.springdoc.core.OpenAPIBuilder;
import org.springdoc.core.OperationBuilder;
import org.springdoc.core.TagsBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponses;

public abstract class AbstractOpenApiResource {

	protected OpenAPIBuilder openAPIBuilder;
	protected AbstractRequestBuilder requestBuilder;
	protected AbstractResponseBuilder responseBuilder;
	protected TagsBuilder tagbuiBuilder;
	protected OperationBuilder operationParser;
	protected InfoBuilder infoBuilder;


	protected AbstractOpenApiResource(OpenAPIBuilder openAPIBuilder, AbstractRequestBuilder requestBuilder,
			AbstractResponseBuilder responseBuilder, TagsBuilder tagbuiBuilder, OperationBuilder operationParser,
			InfoBuilder infoBuilder) {
		super();
		this.openAPIBuilder = openAPIBuilder;
		this.requestBuilder = requestBuilder;
		this.responseBuilder = responseBuilder;
		this.tagbuiBuilder = tagbuiBuilder;
		this.operationParser = operationParser;
		this.infoBuilder = infoBuilder;
	}

	protected void calculatePath(OpenAPIBuilder openAPIBuilder, HandlerMethod handlerMethod, String operationPath,
			Set<RequestMethod> requestMethods) {
		OpenAPI openAPI = openAPIBuilder.getOpenAPI();
		Components components = openAPIBuilder.getComponents();
		Paths paths = openAPIBuilder.getPaths();

		for (RequestMethod requestMethod : requestMethods) {
			// skip hidden operations
			io.swagger.v3.oas.annotations.Operation apiOperation = ReflectionUtils
					.getAnnotation(handlerMethod.getMethod(), io.swagger.v3.oas.annotations.Operation.class);
			if (apiOperation != null && apiOperation.hidden()) {
				continue;
			}

			RequestMapping reqMappringClass = ReflectionUtils.getAnnotation(handlerMethod.getBeanType(),
					RequestMapping.class);

			MediaAttributes mediaAttributes = new MediaAttributes();
			if (reqMappringClass != null) {
				mediaAttributes.setClassConsumes(reqMappringClass.consumes());
				mediaAttributes.setClassProduces(reqMappringClass.produces());
			}

			mediaAttributes.calculateConsumesProduces(requestMethod, handlerMethod.getMethod());

			Operation operation = new Operation();

			// compute tags
			operation = tagbuiBuilder.build(handlerMethod, operation, openAPI);

			// Add documentation from operation annotation
			if (apiOperation != null) {
				openAPI = operationParser.parse(components, apiOperation, operation, openAPI, mediaAttributes);
			}

			// requests
			operation = requestBuilder.build(components, handlerMethod, requestMethod, operation,
					mediaAttributes);

			// responses
			ApiResponses apiResponses = responseBuilder.build(components, handlerMethod, operation,
					mediaAttributes.getAllProduces());

			operation.setResponses(apiResponses);

			PathItem pathItemObject = buildPathItem(requestMethod, operation, operationPath, paths);
			paths.addPathItem(operationPath, pathItemObject);
		}
	}

	protected PathItem buildPathItem(RequestMethod requestMethod, Operation operation, String operationPath,
			Paths paths) {
		PathItem pathItemObject;
		if (paths.containsKey(operationPath)) {
			pathItemObject = paths.get(operationPath);
		} else {
			pathItemObject = new PathItem();
		}
		switch (requestMethod) {
		case POST:
			pathItemObject.post(operation);
			break;
		case GET:
			pathItemObject.get(operation);
			break;
		case DELETE:
			pathItemObject.delete(operation);
			break;
		case PUT:
			pathItemObject.put(operation);
			break;
		case PATCH:
			pathItemObject.patch(operation);
			break;
		case TRACE:
			pathItemObject.trace(operation);
			break;
		case HEAD:
			pathItemObject.head(operation);
			break;
		case OPTIONS:
			pathItemObject.options(operation);
			break;
		default:
			// Do nothing here
			break;
		}
		return pathItemObject;
	}
}
