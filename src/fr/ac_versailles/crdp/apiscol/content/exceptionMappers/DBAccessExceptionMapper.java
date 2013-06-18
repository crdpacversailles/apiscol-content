package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.database.DBAccessException;

@Provider
public class DBAccessExceptionMapper implements ExceptionMapper<DBAccessException> {
	@Override
	public Response toResponse(DBAccessException e) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}