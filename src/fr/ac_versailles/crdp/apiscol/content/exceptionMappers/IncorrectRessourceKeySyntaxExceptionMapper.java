package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.IncorrectResourceKeySyntaxException;

@Provider
public class IncorrectRessourceKeySyntaxExceptionMapper implements
		ExceptionMapper<IncorrectResourceKeySyntaxException> {

	@Override
	public Response toResponse(IncorrectResourceKeySyntaxException e) {
		return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}