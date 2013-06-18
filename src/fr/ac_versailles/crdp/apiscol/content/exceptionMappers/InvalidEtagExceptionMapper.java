package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.InvalidEtagException;

@Provider
public class InvalidEtagExceptionMapper implements
		ExceptionMapper<InvalidEtagException> {

	@Override
	public Response toResponse(InvalidEtagException e) {
		return Response.status(Status.PRECONDITION_FAILED)
				.type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}