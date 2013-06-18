package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.representations.UnknownMediaTypeForResponseException;

@Provider
public class UnknownMediaTypeForResponseExceptionMapper implements
		ExceptionMapper<UnknownMediaTypeForResponseException> {

	@Override
	public Response toResponse(UnknownMediaTypeForResponseException e) {
		return Response.status(Status.NOT_ACCEPTABLE)
				.type(MediaType.APPLICATION_XML).entity(e.getXMLMessage())
				.build();
	}
}