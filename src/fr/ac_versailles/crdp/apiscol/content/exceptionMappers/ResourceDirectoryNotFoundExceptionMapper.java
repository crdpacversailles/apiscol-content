package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;

@Provider
public class ResourceDirectoryNotFoundExceptionMapper implements
		ExceptionMapper<ResourceDirectoryNotFoundException> {
	@Override
	public Response toResponse(ResourceDirectoryNotFoundException e) {
		return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}