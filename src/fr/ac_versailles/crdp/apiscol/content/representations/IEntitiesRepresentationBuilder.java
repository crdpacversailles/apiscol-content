package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;

public interface IEntitiesRepresentationBuilder<T> {

	T getResourceRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException;

	String getResourceStringRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException;

	T getFileSuccessfulDestructionReport(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String fileName);

	T getInexistentFileDestructionAttemptReport(String realPath,
			UriInfo uriInfo, String resourceId, String fileName);

	T getCompleteResourceListRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, int start, int rows, String editUri)
			throws DBAccessException;

	T selectResourceFollowingCriterium(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, ISearchEngineResultHandler handler,
			int start, int rows, String editUri) throws DBAccessException;

	MediaType getMediaType();

	T getResourceSuccessfulDestructionReport(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String warnings);

	T getResourceUnsuccessfulDestructionReport(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId,
			String warnings);

	T getSuccessfullOptimizationReport(String realPath, UriInfo uriInfo);

	T getLinkUpdateProcedureRepresentation(UriInfo uriInfo);

	T getSuccessfulGlobalDeletionReport();

	T getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, UriInfo uriInfo,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException;

	T getResourceTechnicalInformations(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException;

	String getResourcePreviewDirectoryUri(UriInfo uriInfo, String resourceId);

	Object getRefreshProcessRepresentation(Integer refreshProcessIdentifier,
			UriInfo uriInfo, RefreshProcessRegistry refreshProcessRegistry);

}
