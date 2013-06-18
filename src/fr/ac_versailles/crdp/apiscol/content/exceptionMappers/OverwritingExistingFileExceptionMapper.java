package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.OverWritingExistingFileException;

@Provider
public class OverwritingExistingFileExceptionMapper implements
		ExceptionMapper<OverWritingExistingFileException> {

	@Override
	public Response toResponse(OverWritingExistingFileException e) {
		return Response.status(Status.CONFLICT).type(MediaType.APPLICATION_XML)
				.entity(e.getXMLMessage()).build();
	}
}