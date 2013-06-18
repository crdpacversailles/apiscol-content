package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.IllegalResourceTypeChangeException;


@Provider
public class IllegalResourceTypeChangeExceptionMapper implements
		ExceptionMapper<IllegalResourceTypeChangeException> {

	@Override
	public Response toResponse(IllegalResourceTypeChangeException e) {
		return Response.status(422)
				.type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}