package fr.ac_versailles.crdp.apiscol.content;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;

import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.RequestHandler;
import fr.ac_versailles.crdp.apiscol.content.crawler.LinkRefreshingHandler;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.FileSystemAccessException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.representations.AbstractSearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.EntitiesRepresentationBuilderFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.IEntitiesRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.content.representations.UnknownMediaTypeForResponseException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineCommunicationException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineErrorException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SolrJSearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLock;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLockManager;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

@Path("/maintenance")
public class MaintenanceApi {
	// TODO pb d'absence d'initialisation si maintenance est appelée avant toute
	// recherche

	private static Logger logger;
	private static ISearchEngineQueryHandler searchEngineQueryHandler;
	private static boolean isInitialized = false;
	private static KeyLockManager keyLockManager;
	private static ISearchEngineFactory searchEngineFactory;

	public MaintenanceApi(@Context ServletContext context) {
		if (!isInitialized) {
			createLogger();
			createKeyLockManager();
			initializeResourceDirectoryInterface(context);
			createSearchEngineQueryHandler(context);
			isInitialized = true;
		}
	}

	private void createSearchEngineQueryHandler(ServletContext context) {
		String solrAddress = ResourceApi.getProperty(
				ParametersKeys.solrAddress, context);
		String solrSearchPath = ResourceApi.getProperty(
				ParametersKeys.solrSearchPath, context);
		String solrUpdatePath = ResourceApi.getProperty(
				ParametersKeys.solrUpdatePath, context);
		String solrExtractPath = ResourceApi.getProperty(
				ParametersKeys.solrExtractPath, context);
		String solrSuggestPath = ResourceApi.getProperty(
				ParametersKeys.solrSuggestPath, context);
		try {
			searchEngineFactory = AbstractSearchEngineFactory
					.getSearchEngineFactory(AbstractSearchEngineFactory.SearchEngineType.SOLRJ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		searchEngineQueryHandler = searchEngineFactory.getQueryHandler(
				solrAddress, solrSearchPath, solrUpdatePath, solrExtractPath,
				solrSuggestPath);
	}

	private void initializeResourceDirectoryInterface(ServletContext context) {
		if (!ResourceDirectoryInterface.isInitialized())
			ResourceDirectoryInterface.initialize(ResourceApi.getProperty(
					ParametersKeys.fileRepoPath, context), ResourceApi
					.getProperty(ParametersKeys.temporaryFilesPrefix, context));

	}

	private void createKeyLockManager() {
		keyLockManager = KeyLockManager.getInstance();
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());
	}

	/**
	 * Creates a void resource
	 * 
	 * @return resource representation
	 * @throws SearchEngineCommunicationException
	 * @throws SearchEngineErrorException
	 * @throws UnknownMediaTypeForResponseException
	 * @throws DBAccessException
	 * @throws InexistentResourceInDatabaseException
	 * @throws DOMException
	 * @throws FileSystemAccessException
	 * @throws ResourceDirectoryNotFoundException
	 */
	@POST
	@Path("/optimization")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response omptimizeSearchEngineIndex(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		searchEngineQueryHandler.processOptimizationQuery();
		return Response.ok(
				rb.getSuccessfullOptimizationReport(requestedFormat, uriInfo),
				rb.getMediaType()).build();
	}

	private String guessRequestedFormat(HttpServletRequest request,
			String format) {
		// TODO mettre un format par défaut
		if (format == null)
			return RequestHandler.extractAcceptHeader(request);
		else
			return RequestHandler.convertFormatQueryParam(format);
	}

	@POST
	@Path("/link_update_process")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response startLinkUpdateProcedure(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				logger.info("Entering critical section with mutual exclusion for all the content service");
				String requestedFormat = guessRequestedFormat(request, format);
				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(requestedFormat, context);
				IResourceDataHandler resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
				LinkRefreshingHandler.State state = LinkRefreshingHandler
						.getInstance().getCurrentState();
				if (state == LinkRefreshingHandler.State.INACTIVE) {
					LinkRefreshingHandler.getInstance().startUdateProcess(
							searchEngineQueryHandler, resourceDataHandler);
				}
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}
		return Response.ok()
				.entity(rb.getLinkUpdateProcedureRepresentation(uriInfo))
				.build();
	}

	@GET
	@Path("/link_update_process")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response getLinkUpdateProcedureState(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		return Response.ok()
				.entity(rb.getLinkUpdateProcedureRepresentation(uriInfo))
				.build();
	}

	@POST
	@Path("/deletion")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response deleteAllContents(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				ResourceDirectoryInterface.deleteAllFiles();
				searchEngineQueryHandler.deleteIndex();
				IResourceDataHandler resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
				resourceDataHandler.deleteAllDocuments();
				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(
								MediaType.APPLICATION_ATOM_XML, context);
			} finally {
				keyLock.unlock();

			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}

		return Response.ok().entity(rb.getSuccessfulGlobalDeletionReport())
				.build();
	}

	@POST
	@Path("/recovery")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response startRecovery(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			FileSystemAccessException, ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {

				searchEngineQueryHandler.deleteIndex();
				IResourceDataHandler resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
				resourceDataHandler.deleteAllDocuments();
				ArrayList<String> resourceList = ResourceDirectoryInterface
						.getResourcesList();
				Iterator<String> it = resourceList.iterator();

				while (it.hasNext()) {
					String resourceId = it.next();
					String serializedData = ResourceDirectoryInterface
							.getSerializedData(resourceId);
					logger.info("Restoring serialized data " + serializedData
							+ "for resource " + resourceId);
					resourceDataHandler.deserializeAndSaveToDataBase(
							resourceId, serializedData);
					ArrayList<File> files = ResourceDirectoryInterface
							.getFileList(resourceId, true);
					Iterator<File> it2 = files.iterator();
					while (it2.hasNext()) {
						String fileName = it2.next().getName();
						searchEngineQueryHandler.processAddQueryForFile(
								SolrJSearchEngineQueryHandler
										.getDocumentIdentifier(resourceId,
												fileName),
								ResourceDirectoryInterface.getFilePath(
										resourceId, fileName));
						logger.info("Indexing file " + fileName
								+ "for resource " + resourceId);
						searchEngineQueryHandler.processCommitQuery();
					}
				}

				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(
								MediaType.APPLICATION_ATOM_XML, context);
			} finally {
				keyLock.unlock();

			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}

		return Response.ok().entity(rb.getSuccessfulGlobalDeletionReport())
				.build();
	}
}
