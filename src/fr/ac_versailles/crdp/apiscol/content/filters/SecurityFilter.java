package fr.ac_versailles.crdp.apiscol.content.filters;

import javax.servlet.ServletContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.content.ResourceApi;
import fr.ac_versailles.crdp.apiscol.utils.SecurityUtils;

public class SecurityFilter implements ContainerRequestFilter {
	@Context
	ServletContext context;

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		String method = request.getMethod();
		if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.POST)
				|| method.equals(HttpMethod.DELETE)) {
			String secret = ResourceApi.getProperty(ParametersKeys.sharedSecret, context);
			String providedHashedEtag = request
					.getHeaderValue(HttpHeaders.AUTHORIZATION);
			String etag = request.getHeaderValue(HttpHeaders.IF_MATCH);
			String computedHashedEtag=SecurityUtils.hashWithSharedSecret(etag, secret);
			if(!StringUtils.equals(providedHashedEtag, computedHashedEtag))
				 throw new WebApplicationException(Response.status(401).entity("Invalid authorization token in content web service").build());
		}
		return request;
	}
}
