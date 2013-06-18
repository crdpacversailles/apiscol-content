package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.BadFileTypeException;

@Provider
public class BadFileTypeExceptionMapper implements
		ExceptionMapper<BadFileTypeException> {

	@Override
	public Response toResponse(BadFileTypeException e) {
		return Response.status(Status.BAD_REQUEST)
				.type(MediaType.APPLICATION_XML).entity(e.getXMLMessage())
				.build();
	}
}