package fr.ac_versailles.crdp.apiscol.content;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.sun.jersey.multipart.FormDataParam;

import fr.ac_versailles.crdp.apiscol.ApiscolApi;
import fr.ac_versailles.crdp.apiscol.CustomMediaType;
import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.RequestHandler;
import fr.ac_versailles.crdp.apiscol.ResourcesKeySyntax;
import fr.ac_versailles.crdp.apiscol.content.compression.CompressionTask;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.BadFileTypeException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.FileSystemAccessException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.MissingIncomingFileException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.OverWritingExistingFileException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.previews.ConversionServerInterface;
import fr.ac_versailles.crdp.apiscol.content.previews.IPreviewMaker;
import fr.ac_versailles.crdp.apiscol.content.previews.PreviewMakersFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.AbstractSearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.EntitiesRepresentationBuilderFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.IEntitiesRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.content.representations.UnknownMediaTypeForResponseException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineCommunicationException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineErrorException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineTask;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SolrJSearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.UrlBadSyntaxException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.UrlParsingException;
import fr.ac_versailles.crdp.apiscol.content.thumbs.ThumbExtracter;
import fr.ac_versailles.crdp.apiscol.content.thumbs.ThumbExtracterFactory;
import fr.ac_versailles.crdp.apiscol.content.url.InvalidUrlException;
import fr.ac_versailles.crdp.apiscol.content.url.UrlChecker;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLock;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLockManager;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.TimeUtils;

@Path("/")
public class ResourceApi extends ApiscolApi {

	private static String apiscolInstanceName;
	private static String previewsRepoPath;
	private static ExecutorService compressionExecutor;
	private static ExecutorService searchEngineRequestExecutor;
	private static ExecutorService previewMakerExecutor;
	private static ISearchEngineQueryHandler searchEngineQueryHandler;
	private static boolean isInitialized = false;
	private static ISearchEngineFactory searchEngineFactory;
	private static String editUri;
	@Context
	private ServletContext context;
	@Context
	private UriInfo uriInfo;
	private static RefreshProcessRegistry refreshProcessRegistry = new RefreshProcessRegistry();

	public ResourceApi(@Context ServletContext context) {
		super(context);
		if (!isInitialized) {
			initializeStaticParameters();
			initializeResourceDirectoryInterface();
			createSearchEngineQueryHandler();
			createSearchEngineRequestExecutor();
			createCompressionExecutor();
			createPreviewMakerExecutor();
			isInitialized = true;
		}
	}

	private void createSearchEngineQueryHandler() {
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
		logger.warn("ApiScol content creates a new searchenginequery handler.");

		searchEngineQueryHandler = searchEngineFactory.getQueryHandler(
				solrAddress, solrSearchPath, solrUpdatePath, solrExtractPath,
				solrSuggestPath);
	}

	private void initializeResourceDirectoryInterface() {
		if (!ResourceDirectoryInterface.isInitialized())
			ResourceDirectoryInterface.initialize(ResourceApi.getProperty(
					ParametersKeys.fileRepoPath, context), ResourceApi
					.getProperty(ParametersKeys.temporaryFilesPrefix, context));

	}

	private void createCompressionExecutor() {
		if (compressionExecutor == null)
			compressionExecutor = Executors.newSingleThreadExecutor();
	}

	private void createSearchEngineRequestExecutor() {
		if (searchEngineRequestExecutor == null)
			searchEngineRequestExecutor = Executors.newSingleThreadExecutor();
	}

	private void createPreviewMakerExecutor() {
		if (previewMakerExecutor == null)
			previewMakerExecutor = Executors.newFixedThreadPool(5);
	}

	private void initializeStaticParameters() {
		apiscolInstanceName = ResourceApi.getProperty(
				ParametersKeys.apiscolInstanceName, context);
		previewsRepoPath = ResourceApi.getProperty(
				ParametersKeys.previewsRepoPath, context);
		ConversionServerInterface.initialize(context);
	}

	/**
	 * 
	 * @param request
	 *            Http request (injected)
	 * @param resourceId
	 *            Path parameter resid (resource identifier : UUID)
	 * @param uriInfo
	 *            Uriinfo (injected)
	 * @return response object in requested format
	 * @throws DBAccessException
	 * @throws InexistentResourceInDatabaseException
	 * @throws DOMException
	 * @throws IncorrectResourceKeySyntaxException
	 * @throws ResourceDirectoryNotFoundException
	 * @throws UnknownMediaTypeForResponseException
	 */
	@GET
	@Path("/resource/{resid}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getResource(@Context HttpServletRequest request,
			@PathParam(value = "resid") final String resourceId,
			@QueryParam(value = "format") final String format)
			throws DBAccessException, InexistentResourceInDatabaseException,
			IncorrectResourceKeySyntaxException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		return getResourceById(resourceId, request, requestedFormat,
				resourceDataHandler);
	}

	@GET
	@Path("/resource/{resid}/technical_infos")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getResourceTechnicalInformations(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") String resourceIdorUrn,
			@QueryParam(value = "format") final String format)
			throws DBAccessException, InexistentResourceInDatabaseException,
			IncorrectResourceKeySyntaxException,
			ResourceDirectoryNotFoundException, InvalidEtagException,
			UnknownMediaTypeForResponseException {
		String resourceId;
		if (fr.ac_versailles.crdp.apiscol.ResourcesKeySyntax
				.isUrn(resourceIdorUrn))
			resourceId = ResourcesKeySyntax
					.extractResourceIdFromUrn(resourceIdorUrn);
		else
			resourceId = resourceIdorUrn;
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		Object response = rb.getResourceTechnicalInformations(
				realPath(context), uriInfo, apiscolInstanceName, resourceId);

		return Response
				.ok(response, rb.getMediaType())
				.header(HttpHeaders.ETAG,
						getEtatInRFC3339(resourceId, resourceDataHandler))
				.build();
	}

	private Response getResourceById(String resourceId,
			HttpServletRequest request, String requestedFormat,
			IResourceDataHandler resourceDataHandler) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		Object response = rb.getResourceRepresentation(realPath(context),
				uriInfo, apiscolInstanceName, resourceId.toString(),
				ResourceApi.editUri);

		return Response
				.ok(response, rb.getMediaType())
				.header(HttpHeaders.ETAG,
						getEtatInRFC3339(resourceId, resourceDataHandler))
				.build();
	}

	/**
	 * 
	 * Provides a representation of a list of resources. If query parameter and
	 * metadata parameter are blank blank, the whole content of the repository
	 * is listed. If a query is submitted, the list of resources will be
	 * accompanied by information about the results of the research except if
	 * requested format is scorm. For voluminous results, this function supports
	 * pagination.
	 * 
	 * @param request
	 * @param format
	 *            Allows clients to specify the desired format without providing
	 *            HTTP accept header
	 * @param query
	 *            A query whose syntax has to match the requirements of edismax
	 *            query parser. If no query, you will get the full list of
	 *            hosted resources.
	 * @param metadataId
	 *            Filter resources by metadataId
	 * @param fuzzy
	 *            If search words are provided, you can ask for a fuzzy query
	 * @param start
	 *            First result to display (pagination).
	 * @param rows
	 *            Number of rows to display (pagination).
	 * @param context
	 * @param uriInfo
	 * @return Resource list representation with hits and text excerpts in case
	 *         og query with search words
	 * @throws DBAccessException
	 * @throws SearchEngineErrorException
	 * @throws InexistentResourceInDatabaseException
	 * @throws ResourceDirectoryNotFoundException
	 * @throws UnknownMediaTypeForResponseException
	 */

	@GET
	@Path("/resource")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getResourcesList(@Context HttpServletRequest request,
			@QueryParam(value = "format") final String format,
			@QueryParam(value = "query") final String query,
			@QueryParam(value = "mdid") final String metadataId,
			@DefaultValue("0") @QueryParam(value = "fuzzy") final float fuzzy,
			@DefaultValue("0") @QueryParam(value = "start") final int start,
			@DefaultValue("10") @QueryParam(value = "rows") final int rows)
			throws DBAccessException, SearchEngineErrorException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		if (StringUtils.isNotBlank(metadataId)) {
			String resourceId = resourceDataHandler
					.getResourceIdByMetadataId(metadataId);
			return getResourceById(resourceId, request, requestedFormat,
					resourceDataHandler);
		}
		if (StringUtils.isNotBlank(query)) {

			Object result = searchEngineQueryHandler.processSearchQuery(
					query.trim(), fuzzy);
			ISearchEngineResultHandler handler = searchEngineFactory
					.getResultHandler();
			handler.parse(result);
			return Response.ok(
					rb.selectResourceFollowingCriterium(realPath(context),
							uriInfo, apiscolInstanceName, handler, start, rows,
							ResourceApi.editUri), rb.getMediaType()).build();
		}
		return Response
				.ok(rb.getCompleteResourceListRepresentation(realPath(context),
						uriInfo, apiscolInstanceName, start, rows,
						ResourceApi.editUri), rb.getMediaType())
				.type(rb.getMediaType()).build();
	}

	@GET
	@Path("/suggestions")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getQuerySuggestions(@Context HttpServletRequest request,
			@QueryParam(value = "format") final String format,
			@QueryParam(value = "query") final String query)
			throws DBAccessException, SearchEngineErrorException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		if (StringUtils.isBlank(query))
			return Response
					.status(Status.BAD_REQUEST)
					.entity("You  cannot ask for suggestions with a blank query string")
					.type(MediaType.TEXT_PLAIN).build();
		else {
			Object result = searchEngineQueryHandler
					.processSpellcheckQuery(query.trim());
			ISearchEngineResultHandler handler = searchEngineFactory
					.getResultHandler();
			handler.parse(result);
			// TODO valeur magique 10
			return Response.ok(
					rb.selectResourceFollowingCriterium(realPath(context),
							uriInfo, apiscolInstanceName, handler, 0, 10,
							ResourceApi.editUri), rb.getMediaType()).build();
		}
	}

	@GET
	@Path("/resource/{resid}/refresh/{refreshid}")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	public Response getRefreshProcessState(
			@Context HttpServletRequest request,
			@PathParam(value = "refreshid") final Integer refreshProcessIdentifier,
			@PathParam(value = "resid") final String resourceId,
			@QueryParam(value = "format") final String format)
			throws IOException, UnknownMediaTypeForResponseException {

		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		return Response
				.ok()
				.entity(rb.getRefreshProcessRepresentation(
						refreshProcessIdentifier, uriInfo,
						refreshProcessRegistry)).type(rb.getMediaType())
				.build();
	}

	@POST
	@Path("/resource/{resid}/refresh")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response refreshResource(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") String resourceId,
			@FormParam(value = "format") final String format,
			@DefaultValue("") @FormParam("edit_uri") String editUri,
			@DefaultValue("false") @FormParam(value = "archive") final boolean updateArchive,
			@DefaultValue("false") @FormParam(value = "preview") final boolean updatePreview,
			@DefaultValue("false") @FormParam(value = "index") final boolean updateIndex)
			throws DBAccessException, InexistentResourceInDatabaseException,
			IncorrectResourceKeySyntaxException, InvalidEtagException,
			ResourceDirectoryNotFoundException, InvalidUrlException,
			SearchEngineErrorException, UnknownMediaTypeForResponseException,
			IllegalResourceTypeChangeException {
		askForGlobalLock();
		KeyLock keyLock = null;
		if (ResourcesKeySyntax.isUrn(resourceId))
			resourceId = ResourcesKeySyntax
					.extractResourceIdFromUrn(resourceId);
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);

		if (!updateArchive && !updateIndex && !updatePreview) {
			String message = String
					.format("A refresh request was sent for resource %s with all query parameters to false",
							resourceId);
			logger.warn(message);
			return Response.status(Response.Status.BAD_REQUEST).entity(message)
					.build();
		}
		if (updateArchive
				&& ContentType.isLink(resourceDataHandler
						.getScormTypeForResource(resourceId)))
			return Response.status(Status.BAD_REQUEST)
					.entity("There's no archive to update for links").build();
		ResponseBuilder response = null;
		try {
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for resource %s",
								resourceId));
				if (!StringUtils.isEmpty(editUri))
					ResourceApi.editUri = editUri;
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));
				Integer refreshProcessIdentifier = -1;
				if (updateArchive
						&& ContentType.isFile(resourceDataHandler
								.getScormTypeForResource(resourceId))
						&& ResourceDirectoryInterface
								.resourceHasFiles(resourceId))
					refreshProcessIdentifier = updateResourceDownloadableArchive(
							resourceId, request);

				if (updatePreview)
					refreshProcessIdentifier = updateResourcePreview(
							resourceId, request, resourceDataHandler,
							rb.getResourcePreviewDirectoryUri(uriInfo,
									resourceId));

				if (updateIndex)
					refreshProcessIdentifier = updateResourceInSearchEngineIndex(
							resourceId, request, resourceDataHandler);
				response = Response.ok(rb.getRefreshProcessRepresentation(
						refreshProcessIdentifier, uriInfo,
						refreshProcessRegistry));

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}

		return response.build();
	}

	@PUT
	@Path("/resource/{resid}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response updateResourceProperties(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") String resourceId,
			@FormParam(value = "format") final String format,
			@FormParam(value = "mdid") final String metadataId,
			@FormParam(value = "main_filename") final String mainFileName,
			@FormParam(value = "url") String url,
			@FormParam(value = "type") final String scormType,
			@DefaultValue("") @FormParam("edit_uri") String editUri,
			@DefaultValue("true") @FormParam(value = "update_archive") final boolean updateArchive,
			@DefaultValue("true") @FormParam(value = "update_preview") final boolean updatePreview)
			throws DBAccessException, InexistentResourceInDatabaseException,
			IncorrectResourceKeySyntaxException, InvalidEtagException,
			ResourceDirectoryNotFoundException, InvalidUrlException,
			SearchEngineErrorException, UnknownMediaTypeForResponseException,
			IllegalResourceTypeChangeException {
		askForGlobalLock();
		KeyLock keyLock = null;
		if (ResourcesKeySyntax.isUrn(resourceId))
			resourceId = ResourcesKeySyntax
					.extractResourceIdFromUrn(resourceId);
		// TODO vérifier ce qui se passe si on modifie une propté puis exception
		// sur la suivante
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);

		if (StringUtils.isBlank(metadataId)
				&& StringUtils.isBlank(mainFileName)
				&& StringUtils.isBlank(url) && StringUtils.isBlank(scormType)
				&& !updateArchive) {
			String message = String
					.format("A put request was sent for resource %s with all query parameters blank",
							resourceId);
			logger.warn(message);
			return Response.status(Response.Status.BAD_REQUEST).entity(message)
					.build();
		}
		ResponseBuilder response = null;
		try {
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for resource %s",
								resourceId));
				if (!StringUtils.isEmpty(editUri))
					ResourceApi.editUri = editUri;
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));
				boolean versionNumberHasToBeUpdated = false;
				if (StringUtils.isNotBlank(mainFileName)) {
					if (StringUtils.equals(resourceDataHandler
							.getScormTypeForResource(resourceId),
							ContentType.url.toString())) {
						String message = String
								.format("A put request was sent for resource %s with a mainFileName parameter %s but this resource is of type url",
										resourceId, mainFileName);
						logger.warn(message);
					} else {
						if (ResourceDirectoryInterface.existsFile(resourceId,
								mainFileName)) {
							resourceDataHandler.setMainFileForResource(
									resourceId, mainFileName);
							versionNumberHasToBeUpdated = true;
						} else {
							String message = String
									.format("A put request was sent for resource %s with a mainFileName %s parameter but this file is absent of the resource directory",
											resourceId, mainFileName);
							logger.warn(message);
							response = Response.status(422).entity(message);
						}
					}

				}

				if (response == null && StringUtils.isNotBlank(metadataId)) {
					resourceDataHandler.setMetadataForResource(resourceId,
							metadataId);
					versionNumberHasToBeUpdated = true;
				}

				if (response == null && StringUtils.isNotBlank(scormType)) {
					String actualScormType = resourceDataHandler
							.getScormTypeForResource(resourceId);
					if (ContentType.isFile(actualScormType)
							&& ContentType.isLink(scormType)
							&& ResourceDirectoryInterface
									.resourceHasFiles(resourceId)) {
						throw new IllegalResourceTypeChangeException(
								actualScormType, resourceId);
					} else {
						resourceDataHandler.setScormTypeForResource(
								resourceId.toString(),
								ContentType.convertStringToType(scormType));
						versionNumberHasToBeUpdated = true;
						if (ContentType.isLink(actualScormType)
								&& ContentType.isFile(scormType)) {
							String actualUrl = resourceDataHandler
									.getUrlForResource(resourceId);
							if (StringUtils.isNotBlank(actualUrl)) {
								resourceDataHandler
										.eraseUrlForResource(resourceId);
								try {
									searchEngineQueryHandler
											.processDeleteQuery(SolrJSearchEngineQueryHandler
													.getDocumentIdentifier(
															resourceId,
															actualUrl));
									searchEngineQueryHandler
											.processCommitQuery();
								} catch (SearchEngineErrorException e) {
									logger.error(String
											.format("A search engine error occured while trying to remove url %s for resource %s from search engine index with message %s",
													actualUrl, resourceId,
													e.getMessage()));
								} catch (SearchEngineCommunicationException e) {
									logger.error(String
											.format("A communication problem with search engine occured while trying to remove url %s for resource %s from search engine index with message %s",
													actualUrl, resourceId,
													e.getMessage()));
								}
								logger.info(String
										.format("Asking solr to remove from the index the actual url %s for resource %s.",
												actualUrl, resourceId));
							}

						} else if (ContentType.isFile(actualScormType)
								&& ContentType.isLink(scormType)) {
							String actualMainFile = resourceDataHandler
									.getMainFileForResource(resourceId);
							if (StringUtils.isNotBlank(actualMainFile)) {
								logger.error(String
										.format("The actualMainField field in mongo contains %s value, but it should be blank because the resource %s is about to be converted into url type.",
												actualMainFile, resourceId));
							}
							resourceDataHandler
									.eraseMainFileForResource(resourceId);
						}

					}
				}

				if (response == null && StringUtils.isNotBlank(url)) {
					try {
						url = URLDecoder.decode(url, "UTF-8");
					} catch (UnsupportedEncodingException e1) {

						e1.printStackTrace();
						throw new InvalidUrlException(
								String.format(
										"The url %s is not acceptable for this reason : %s",
										url, e1.getMessage()));
					}
					UrlChecker.checkUrlSyntax(url);
					String actualScormType = resourceDataHandler
							.getScormTypeForResource(resourceId);
					if (ContentType.isLink(actualScormType)) {
						String actualUrl = resourceDataHandler
								.getUrlForResource(resourceId);
						if (StringUtils.isNotBlank(actualUrl))
							try {
								searchEngineQueryHandler
										.processDeleteQuery(SolrJSearchEngineQueryHandler
												.getDocumentIdentifier(
														resourceId, actualUrl));
							} catch (SearchEngineErrorException e) {
								logger.error(String
										.format("A search engine error occured while trying to remove url %s for resource %s from search engine index with message %s",
												actualUrl, resourceId,
												e.getMessage()));
							} catch (SearchEngineCommunicationException e) {
								logger.error(String
										.format("A communication problem with search engine occured while trying to remove url %s for resource %s from search engine index with message %s",
												actualUrl, resourceId,
												e.getMessage()));
							}
						try {
							searchEngineQueryHandler.processAddQueryForUrl(
									SolrJSearchEngineQueryHandler
											.getDocumentIdentifier(resourceId,
													url), url);
						} catch (UrlBadSyntaxException e) {
							response = Response.status(Status.BAD_REQUEST)
									.entity(e.getMessage().toString())
									.type(MediaType.TEXT_PLAIN);
						} catch (UrlParsingException e) {
							response = Response.status(422)
									.entity(e.getMessage().toString())
									.type(MediaType.TEXT_PLAIN);
						} catch (SearchEngineCommunicationException e) {
							response = Response
									.status(Status.INTERNAL_SERVER_ERROR)
									.entity(e.getMessage().toString())
									.type(MediaType.TEXT_PLAIN);
						}
						if (response == null) {
							resourceDataHandler.setUrlForResource(
									resourceId.toString(), url);
							versionNumberHasToBeUpdated = true;
							try {
								searchEngineQueryHandler.processCommitQuery();
							} catch (SearchEngineErrorException e) {
								logger.error(String
										.format("A search engine error occured while trying to commit search engine for resource index with message %s",
												resourceId, e.getMessage()));
							} catch (SearchEngineCommunicationException e) {
								logger.error(String
										.format("A communication problem with search engine occured while trying to commit search engine for resource index with message %s",
												resourceId, e.getMessage()));
							}
						}

					} else {
						String message = String
								.format("You cannot assign the url %s to the resource %s which is of content type %s",
										url, resourceId, actualScormType);
						logger.warn(message);
					}

				}
				if (updateArchive
						&& ContentType.isFile(resourceDataHandler
								.getScormTypeForResource(resourceId))
						&& ResourceDirectoryInterface
								.resourceHasFiles(resourceId))
					updateResourceDownloadableArchive(resourceId, request);
				// uodate the preview if you changed the main file for a file
				// or of you changed the url for a remote resource
				boolean previewHasToBeUpdated = updatePreview;

				if (updatePreview) {
					if (ContentType.isLink(resourceDataHandler
							.getScormTypeForResource(resourceId)))
						previewHasToBeUpdated = previewHasToBeUpdated
								|| StringUtils.isNotEmpty(url);
					if (ContentType.isFile(resourceDataHandler
							.getScormTypeForResource(resourceId)))
						previewHasToBeUpdated = previewHasToBeUpdated
								|| StringUtils.isNotEmpty(mainFileName);
				}
				if (previewHasToBeUpdated)
					updateResourcePreview(resourceId, request,
							resourceDataHandler,
							rb.getResourcePreviewDirectoryUri(uriInfo,
									resourceId));
				if (response == null) {
					if (versionNumberHasToBeUpdated)
						resourceDataHandler.updateVersionNumber(resourceId);
					response = Response.ok(
							rb.getResourceRepresentation(realPath(context),
									uriInfo, apiscolInstanceName, resourceId,
									ResourceApi.editUri), rb.getMediaType())
							.header(HttpHeaders.ETAG,
									getEtatInRFC3339(resourceId,
											resourceDataHandler));
				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}

		return response.build();
	}

	private Object getEtatInRFC3339(String resourceId,
			IResourceDataHandler resourceDataHandler)
			throws InexistentResourceInDatabaseException, DBAccessException {
		try {
			return TimeUtils.toRFC3339(Long.parseLong(resourceDataHandler
					.getEtagForResource(resourceId)));
		} catch (NumberFormatException e) {
			return TimeUtils.toRFC3339(0L);
		}
	}

	private void checkFreshness(String providedEtag, String storedEtag)
			throws InvalidEtagException {
		storedEtag = storedEtag.replace("data-version-", "");
		try {
			if (!StringUtils.equals(providedEtag,
					TimeUtils.toRFC3339(Long.parseLong(storedEtag))))
				throw new InvalidEtagException(providedEtag);
		} catch (NumberFormatException e) {
			logger.error(String.format("Stored etag %s is not a valid etag",
					storedEtag));
		}
	}

	/**
	 * Creates a void resource
	 * 
	 * @return resource representation
	 * @throws DBAccessException
	 * @throws InexistentResourceInDatabaseException
	 * @throws DOMException
	 * @throws FileSystemAccessException
	 * @throws UnknownMediaTypeForResponseException
	 * @throws ResourceDirectoryNotFoundException
	 */
	@POST
	@Path("/resource")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response createResource(@Context HttpServletRequest request,
			@FormParam(value = "mdid") final String metadataId,
			@FormParam(value = "type") final String scormType,
			@DefaultValue("") @FormParam("edit_uri") String editUri)
			throws DBAccessException, FileSystemAccessException,
			UnknownMediaTypeForResponseException {
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		askForGlobalLock();
		KeyLock keyLock = null;

		UUID resourceId = UUID.randomUUID();
		try {
			logger.info(String
					.format("Entering critical section with mutual exclusion for resource %s",
							resourceId));
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {
				if (!StringUtils.isEmpty(editUri))
					ResourceApi.editUri = editUri;
				logger.info("Creating void resource with id : " + resourceId);
				ResourceDirectoryInterface.createDirectory(resourceId);
				resourceDataHandler.createResourceEntry(resourceId.toString());
				// The method ContentType.convertStringToType will provide a
				// default type if necessary
				ContentType filteredScormType = ContentType
						.convertStringToType(scormType);
				try {
					resourceDataHandler.setScormTypeForResource(
							resourceId.toString(), filteredScormType);
					if (StringUtils.isNotBlank(metadataId))
						resourceDataHandler.setMetadataForResource(
								resourceId.toString(), metadataId);
				} catch (InexistentResourceInDatabaseException e) {
					String message = String
							.format("Impossible to update scorm type and mdid of the resource %s that where just created",
									resourceId);
					logger.error(message);
					throw new DBAccessException(message);
				}
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(
						RequestHandler.extractAcceptHeader(request), context);
		Object response;
		try {
			response = rb.getResourceRepresentation(realPath(context), uriInfo,
					apiscolInstanceName, resourceId.toString(),
					ResourceApi.editUri);
		} catch (InexistentResourceInDatabaseException e1) {
			String message = String
					.format("Impossible get the data of the resource %s that where just created",
							resourceId);
			logger.error(message);
			throw new DBAccessException(message);
		} catch (ResourceDirectoryNotFoundException e1) {
			String message = String
					.format("Impossible to find the directory of the resource %s that where just created",
							resourceId);
			logger.error(message);
			throw new FileSystemAccessException(message);
		}
		try {
			return Response
					.status(com.sun.jersey.api.client.ClientResponse.Status.CREATED)
					.entity(response)
					.type(rb.getMediaType())
					.header(HttpHeaders.ETAG,
							getEtatInRFC3339(resourceId.toString(),
									resourceDataHandler)).build();
		} catch (InexistentResourceInDatabaseException e) {
			String message = String
					.format("Impossible to get the version number of the resource %s that where just created",
							resourceId);
			logger.error(message);
			throw new DBAccessException(message);
		}
	}

	@PUT
	@Path("/resource/{resid}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response addContent(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") String resourceId,
			@FormDataParam(value = "file_name") final String fileName,
			@FormDataParam(value = "main_filename") final String mainFileName,
			@DefaultValue("false") @FormDataParam(value = "main") final boolean main,
			@FormDataParam(value = "format") final String format,
			@DefaultValue("true") @FormDataParam(value = "update_archive") final boolean updateArchive,
			@DefaultValue("true") @FormDataParam(value = "update_preview") final boolean updatePreview,
			@DefaultValue("true") @FormDataParam(value = "extract_thumbs") final boolean extractThumbs,
			@DefaultValue("") @FormDataParam("edit_uri") String editUri,
			@DefaultValue("false") @FormDataParam(value = "is_archive") final boolean isArchive)
			throws DBAccessException, FileSystemAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException, InvalidEtagException,
			MissingIncomingFileException, IncorrectResourceKeySyntaxException,
			OverWritingExistingFileException, BadFileTypeException,
			UnknownMediaTypeForResponseException {
		if (ResourcesKeySyntax.isUrn(resourceId))
			resourceId = ResourcesKeySyntax
					.extractResourceIdFromUrn(resourceId);
		if (!StringUtils.isEmpty(editUri))
			ResourceApi.editUri = editUri;
		if (isArchive)
			return addArchive(request, resourceId, fileName, mainFileName,
					format, updateArchive, updatePreview);
		else
			return addFile(request, format, main, resourceId, fileName,
					updateArchive, updatePreview, extractThumbs);
	}

	private Response addArchive(HttpServletRequest request,
			final String resourceId, final String archiveName,
			final String mainFileName, final String format,
			final boolean updateArchive, final boolean updatePreview)
			throws DBAccessException, FileSystemAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException, InvalidEtagException,
			MissingIncomingFileException, BadFileTypeException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		ResponseBuilder response = null;
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		askForGlobalLock();
		KeyLock keyLock = null;

		try {
			logger.info(String
					.format("Entering critical section with mutual exclusion for resource %s",
							resourceId));
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {

				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));

				ResourceDirectoryInterface.registerArchive(resourceId,
						archiveName);

				resourceDataHandler.updateVersionNumber(resourceId);

				ArrayList<String> list = ResourceDirectoryInterface
						.getFileNamesList(resourceId);
				Iterator<String> it = list.iterator();
				String fileName = null;
				while (it.hasNext()) {
					fileName = (String) it.next();
					try {
						searchEngineQueryHandler.processAddQueryForFile(
								SolrJSearchEngineQueryHandler
										.getDocumentIdentifier(resourceId,
												fileName),
								ResourceDirectoryInterface.getFilePath(
										resourceId, fileName));
						searchEngineQueryHandler.processCommitQuery();
					} catch (SearchEngineErrorException e) {
						logger.error(String
								.format("A search engine error occured while trying to add file %s for resource %s from search engine index with message %s",
										fileName, resourceId, e.getMessage()));
					} catch (SearchEngineCommunicationException e) {
						logger.error(String
								.format("A communication problem with search engine occured while trying to add file %s for resource %s from search engine index with message %s",
										fileName, resourceId, e.getMessage()));
					}

					if (!fileName.contains("/")
							&& StringUtils.isBlank(resourceDataHandler
									.getMainFileForResource(resourceId)))
						resourceDataHandler.setMainFileForResource(resourceId,
								fileName);
				}
				if (!StringUtils.isBlank(fileName)
						&& StringUtils.isBlank(resourceDataHandler
								.getMainFileForResource(resourceId)))
					resourceDataHandler.setMainFileForResource(resourceId,
							fileName);
				if (updateArchive)
					updateResourceDownloadableArchive(resourceId, request);
				if (updatePreview)
					updateResourcePreview(resourceId, request,
							resourceDataHandler,
							rb.getResourcePreviewDirectoryUri(uriInfo,
									resourceId));
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}
		if (response == null)
			response = Response
					.ok(response, rb.getMediaType())
					.entity(rb.getResourceRepresentation(realPath(context),
							uriInfo, apiscolInstanceName,
							resourceId.toString(), ResourceApi.editUri))
					.header(HttpHeaders.ETAG,
							getEtatInRFC3339(resourceId, resourceDataHandler));
		return response.build();
	}

	private Response addFile(HttpServletRequest request, final String format,
			final boolean main, final String resourceId, final String fileName,
			final boolean updateArchive, final boolean updatePreview,
			final boolean extractThumbs) throws DBAccessException,
			IncorrectResourceKeySyntaxException, MissingIncomingFileException,
			InexistentResourceInDatabaseException,
			OverWritingExistingFileException, InvalidEtagException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		ResponseBuilder response = null;
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		askForGlobalLock();
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for resource %s",
								resourceId));
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));
				try {
					ResourceDirectoryInterface.registerFile(resourceId,
							fileName);
				} catch (OverWritingExistingFileException e) {
					ResourceDirectoryInterface.deleteTemporaryFile(resourceId,
							fileName);
					throw e;
				}

				if (!main)
					resourceDataHandler
							.reportFileAddition(resourceId, fileName);
				else
					resourceDataHandler.setMainFileForResource(resourceId,
							fileName);
				resourceDataHandler.updateVersionNumber(resourceId);
				try {
					searchEngineQueryHandler
							.processAddQueryForFile(
									SolrJSearchEngineQueryHandler
											.getDocumentIdentifier(resourceId,
													fileName),
									ResourceDirectoryInterface.getFilePath(
											resourceId, fileName));
					searchEngineQueryHandler.processCommitQuery();
				} catch (SearchEngineErrorException e) {
					logger.error(String
							.format("A search engine error occured while trying to add file %s for resource %s from search engine index with message %s",
									fileName, resourceId, e.getMessage()));
				} catch (SearchEngineCommunicationException e) {
					logger.error(String
							.format("A communication problem with search engine occured while trying to add file %s for resource %s from search engine index with message %s",
									fileName, resourceId, e.getMessage()));
				}

				if (updateArchive)
					updateResourceDownloadableArchive(resourceId, request);
				if (updatePreview)
					updateResourcePreview(resourceId, request,
							resourceDataHandler,
							rb.getResourcePreviewDirectoryUri(uriInfo,
									resourceId));
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}
		if (response == null)
			response = Response.ok(
					rb.getResourceRepresentation(realPath(context), uriInfo,
							apiscolInstanceName, resourceId,
							ResourceApi.editUri), rb.getMediaType()).header(
					HttpHeaders.ETAG,
					getEtatInRFC3339(resourceId, resourceDataHandler));
		return response.build();

	}

	private Integer updateResourceDownloadableArchive(String resourceId,
			HttpServletRequest request) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {
		Integer identifier = -1;
		Document manifestFile = (Document) EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(CustomMediaType.SCORM_XML.toString(),
						context).getResourceRepresentation(realPath(context),
						uriInfo, apiscolInstanceName, resourceId,
						ResourceApi.editUri);
		CompressionTask task = null;
		try {
			task = new CompressionTask(resourceId, manifestFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized (refreshProcessRegistry) {
			identifier = refreshProcessRegistry.register(task, resourceId);
		}
		compressionExecutor.execute(task);
		return identifier;
	}

	private Integer updateResourcePreview(String resourceId,
			HttpServletRequest request,
			IResourceDataHandler resourceDataHandler, String previewUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		// TODO delete previous
		IPreviewMaker task = null;
		Integer identifier = -1;
		String mimeType;
		String resourceType = resourceDataHandler
				.getScormTypeForResource(resourceId);
		boolean isRemote = resourceType.equals(ContentType.url.toString());
		String entryPoint = "";
		if (isRemote) {
			mimeType = MediaType.WILDCARD_TYPE.toString();
			entryPoint = resourceDataHandler.getUrlForResource(resourceId);
		} else {
			String mainFile = resourceDataHandler
					.getMainFileForResource(resourceId);
			entryPoint = mainFile;
			mimeType = ResourceDirectoryInterface.getMimeType(resourceId,
					mainFile);
		}
		task = PreviewMakersFactory.getPreviewMaker(mimeType, resourceId,
				previewsRepoPath, entryPoint, isRemote, realPath(context),
				previewUri);
		if (task != null) {
			synchronized (refreshProcessRegistry) {
				identifier = refreshProcessRegistry.register(task, resourceId);
			}
			previewMakerExecutor.execute(task);
		} else {
			// TODO gérer pas de previwew
		}
		return identifier;
	}

	private Integer updateResourceInSearchEngineIndex(String resourceId,
			HttpServletRequest request, IResourceDataHandler resourceDataHandler)
			throws ResourceDirectoryNotFoundException {
		Integer identifier = -1;
		IRefreshProcess task = null;
		try {
			task = new SearchEngineTask(resourceId, resourceDataHandler,
					searchEngineQueryHandler);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized (refreshProcessRegistry) {
			identifier = refreshProcessRegistry.register(task, resourceId);
		}
		searchEngineRequestExecutor.execute(task);
		return identifier;
	}

	private void deleteResourcePreview(String resourceId,
			String resourcePreviewDirectoryUri) {
		String previewDirectoryPath = FileUtils.getFilePathHierarchy(
				previewsRepoPath, resourceId);
		if (!FileUtils.deleteDir(new File(previewDirectoryPath)))
			logger.error(String.format(
					"Unable to delete preview directory %s for resource %s",
					previewDirectoryPath, resourceId));

	}

	@DELETE
	@Path("/resource/{resid}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response deleteContent(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") final String resourceId,
			@QueryParam(value = "format") final String format,
			@QueryParam(value = "fname") final String fileName,
			@DefaultValue("true") @QueryParam(value = "update_archive") final boolean updateArchive,
			@DefaultValue("true") @QueryParam(value = "update_preview") final boolean updatePreview)
			throws DBAccessException, ResourceDirectoryNotFoundException,
			IncorrectResourceKeySyntaxException,
			InexistentResourceInDatabaseException, InvalidEtagException,
			FileSystemAccessException, UnknownMediaTypeForResponseException {

		if (StringUtils.isBlank(fileName))
			return deleteResource(request, format, resourceId);
		else
			return deleteFile(request, format, resourceId, fileName,
					updateArchive, updatePreview);

	}

	private Response deleteFile(HttpServletRequest request,
			final String format, final String resourceId,
			final String fileName, final boolean updateArchive,
			boolean updatePreview) throws DBAccessException,
			ResourceDirectoryNotFoundException,
			IncorrectResourceKeySyntaxException,
			InexistentResourceInDatabaseException, InvalidEtagException,
			UnknownMediaTypeForResponseException {
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		askForGlobalLock();
		ResponseBuilder response = null;
		try {
			keyLock = keyLockManager.getLock(resourceId);
			keyLock.lock();
			try {
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));
				logger.info(String
						.format("Entering critical section with mutual exclusion for resource %s",
								resourceId));
				if (!ResourceDirectoryInterface
						.existsResourceDirectory(resourceId)) {
					Object message = String
							.format("An attempt was made to add file %s but directory was not found for the resource %s",
									fileName, resourceId);
					logger.error(message);
					response = Response.status(404).entity(message);
				} else {
					logger.info(String
							.format("The directory for the resource %s has been found on the file system",
									resourceId));
					String scormType = null;
					try {
						scormType = resourceDataHandler
								.getScormTypeForResource(resourceId);
					} catch (InexistentResourceInDatabaseException e) {
						logger.info(String
								.format("No entry was found in the database for resource %s, trying to create a default one",
										resourceId));
						resourceDataHandler.createResourceEntry(resourceId,
								ContentType.asset);
					}
					if (ContentType.isLink(scormType)) {
						String message = String
								.format("Impossible to delete the file %s for the resource %s, the ressource is of the wrong type (url), it has nos files.",
										fileName, resourceId);
						logger.warn(message);
						response = Response.status(422).entity(message);
					} else {
						String decodedFileName = null;
						try {
							decodedFileName = URLDecoder.decode(fileName,
									"UTF-8");
						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Boolean success = ResourceDirectoryInterface
								.deleteFile(resourceId, decodedFileName);
						if (success) {
							logger.info(String
									.format("Successfully deleted the file %s for the resource %s",
											decodedFileName, resourceId));
							try {
								searchEngineQueryHandler
										.processDeleteQuery(SolrJSearchEngineQueryHandler
												.getDocumentIdentifier(
														resourceId,
														decodedFileName));
								searchEngineQueryHandler.processCommitQuery();
							} catch (SearchEngineErrorException e) {
								logger.error(String
										.format("A search engine error occured while trying to remove file %s for resource %s from search engine index with message %s",
												decodedFileName, resourceId,
												e.getMessage()));
							} catch (SearchEngineCommunicationException e) {
								logger.error(String
										.format("A communication problem with search engine occured while trying to remove file %s for resource %s from search engine index with message %s",
												decodedFileName, resourceId,
												e.getMessage()));
							}

							// If the deleted file was the main one, it has to
							// be erased and eventually replaced by another one

							try {
								resourceDataHandler.checkMainFileOnResource(
										resourceId, ResourceDirectoryInterface
												.getFileNamesList(resourceId));
								resourceDataHandler
										.updateVersionNumber(resourceId);
							} catch (InexistentResourceInDatabaseException e) {
								logger.info(String
										.format("No entry was found in the database for resource %s, trying to create a default one",
												resourceId));
								resourceDataHandler.createResourceEntry(
										resourceId, ContentType.asset);
							}
							rb = EntitiesRepresentationBuilderFactory
									.getRepresentationBuilder(requestedFormat,
											context);
							// TODO getFileSuccessfulDestructionReport sert à
							// rien
							response = Response.ok(
									rb.getFileSuccessfulDestructionReport(
											realPath(context), uriInfo,
											apiscolInstanceName, resourceId,
											fileName), rb.getMediaType())
									.header(HttpHeaders.ETAG,
											getEtatInRFC3339(resourceId,
													resourceDataHandler));
							if (!ResourceDirectoryInterface
									.resourceHasFiles(resourceId)) {
								boolean successFullDirectoryDeletion = ResourceDirectoryInterface
										.deleteResourceArchive(resourceId);
								if (!successFullDirectoryDeletion) {
									String errorReport = String
											.format("Resource %s doesn't contains any file more but it was impossible to delete his archive",
													resourceId);
									logger.error(errorReport);
								}
							} else if (updateArchive)
								updateResourceDownloadableArchive(resourceId,
										request);

						} else {
							logger.warn(String
									.format("Failed to delete the file %s for the resource %s",
											fileName, resourceId));
							Boolean exists = ResourceDirectoryInterface
									.existsFile(resourceId, fileName);
							if (exists) {
								logger.error(String
										.format("The file %s for the resource %s seems to exists, check permissions",
												fileName, resourceId));

								response = Response
										.serverError()
										.entity("Impossible to  destroy the file. Thre may be a unix permissions problem.");
							} else {
								logger.warn(String
										.format("Deletion of the file %s for the resource %s has been requested but it does not exist",
												fileName, resourceId));
								try {
									searchEngineQueryHandler
											.processDeleteQuery(SolrJSearchEngineQueryHandler
													.getDocumentIdentifier(
															resourceId,
															fileName));
									searchEngineQueryHandler
											.processCommitQuery();
								} catch (SearchEngineErrorException e) {
									logger.error(String
											.format("A search engine error occured while trying to remove file %s for resource %s from search engine index with message %s",
													fileName, resourceId,
													e.getMessage()));
								} catch (SearchEngineCommunicationException e) {
									logger.error(String
											.format("A communication problem with search engine occured while trying to remove file %s for resource %s from search engine index with message %s",
													fileName, resourceId,
													e.getMessage()));
								}

								logger.warn(String
										.format("For consistency reason, the file %s of the resource %s will be removed from the search engine index",
												fileName, resourceId));
								response = Response
										.status(Status.NOT_FOUND)
										.entity(String
												.format("There is no file with the name %s for the resource %s",
														fileName, resourceId));
							}
						}
					}
					if (updatePreview)
						updateResourcePreview(resourceId, request,
								resourceDataHandler,
								rb.getResourcePreviewDirectoryUri(uriInfo,
										resourceId));
				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}
		return response.build();
	}

	private void askForGlobalLock() {
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				logger.info(String
						.format("Passing through mutual exclusion for all the content service"));
			} finally {
				keyLock.unlock();

			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
		}

	}

	public Response deleteResource(HttpServletRequest request,
			final String format, final String resourceId)
			throws DBAccessException, IncorrectResourceKeySyntaxException,
			InvalidEtagException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException, FileSystemAccessException,
			UnknownMediaTypeForResponseException {
		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		ResponseBuilder response = null;
		StringBuilder warnings = new StringBuilder();
		askForGlobalLock();
		KeyLock keyLock = null;

		try {
			keyLock = keyLockManager.getLock(resourceId.toString());
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for resource %s",
								resourceId));
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						resourceDataHandler.getEtagForResource(resourceId));
				String scormType = null;
				String url = null;
				boolean entryExistsInDatabase = true;
				boolean searchEngineWaitingForCommit = false;
				boolean successFullDirectoryDeletion = false;
				// dont't clean the database information
				// while resource directory is not deleted
				try {
					// before deleting, fetch url and scormtype
					scormType = resourceDataHandler
							.getScormTypeForResource(resourceId);
					url = resourceDataHandler.getUrlForResource(resourceId);
				} catch (InexistentResourceInDatabaseException e) {

					// the resource does not exist in database
					entryExistsInDatabase = false;
					String errorReport = String
							.format("No entry was found in the database for resource %s to be deleted",
									resourceId);
					warnings.append(errorReport);
					logger.error(errorReport);
				}
				// url and scormtype may be undefined at this stage
				// now try to delete the resource directory
				boolean existsResourceDirectory = ResourceDirectoryInterface
						.existsResourceDirectory(resourceId);
				if (existsResourceDirectory) {
					// the second parameter is to get the @ignore files too
					ArrayList<String> files = null;
					try {
						files = ResourceDirectoryInterface.getFileNamesList(
								resourceId, false);
					} catch (ResourceDirectoryNotFoundException e1) {
						logger.error("Incoherent situation. File exists but ResourceDirectoryNotFoundException has trigerred");
						throw e1;
					}
					Boolean successfullFileCollectionDelection = true;
					Iterator<String> it = null;
					try {
						it = files.iterator();
					} catch (Exception e) {
						throw new ResourceDirectoryNotFoundException(resourceId);
					}

					String fileName;
					boolean successFullFileDeletion = false;
					while (it.hasNext()) {
						// TODO faire une première passe avec canwrite
						fileName = it.next();
						if (ResourceDirectoryInterface.isIgnoredFile(fileName)) {
							String warning = String
									.format("The file %s is a temporary file, it should not remain in the %s directory. It while be deleted anyway.",
											fileName, resourceId);
							logger.warn(warning);
							warnings.append(warning);
						}
						// try to delete the file from filesystem
						try {
							successFullFileDeletion = ResourceDirectoryInterface
									.deleteFile(resourceId, fileName);
						} catch (ResourceDirectoryNotFoundException e) {
							logger.error("Incoherent situation. File exists but ResourceDirectoryNotFoundException has trigerred");
							throw e;
						}

						if (successFullFileDeletion) {
							logger.info(String
									.format("Successfully deleted the file %s for the resource %s",
											fileName, resourceId));
						} else {
							String errorReport = "Failed to delete the file %s for the resource %s";
							warnings.append(errorReport);
							logger.warn(String.format(errorReport, fileName,
									resourceId));
							Boolean exists = ResourceDirectoryInterface
									.existsFile(resourceId, fileName);
							if (exists) {
								String errorReport1 = "The file %s for the resource %s seems to exists, check permissions";
								warnings.append(errorReport1);
								logger.error(String.format(errorReport1,
										fileName, resourceId));

							} else {
								String errorReport2 = String
										.format("Deletion of the file %s for the resource %s has been requested but it is not accessible",
												fileName, resourceId);
								warnings.append(errorReport2);
								logger.warn(errorReport2);
							}
							throw new FileSystemAccessException(
									warnings.toString());
						}
						// if successfull deletion remove the file from
						// solrindex
						if (successFullFileDeletion
								&& !ResourceDirectoryInterface
										.isIgnoredFile(fileName)) {
							try {
								searchEngineQueryHandler
										.processDeleteQuery(SolrJSearchEngineQueryHandler
												.getDocumentIdentifier(
														resourceId, fileName));
							} catch (SearchEngineErrorException e) {
								logger.error(String
										.format("A search engine error occured while trying to remove file %s for resource %s from search engine index with message %s",
												fileName, resourceId,
												e.getMessage()));
							} catch (SearchEngineCommunicationException e) {
								logger.error(String
										.format("A communication problem with search engine occured while trying to remove file %s for resource %s from search engine index with message %s",
												fileName, resourceId,
												e.getMessage()));
							}
							searchEngineWaitingForCommit = true;
						}
						successfullFileCollectionDelection &= successFullFileDeletion;
					}
					if (successfullFileCollectionDelection)
						if (ResourceDirectoryInterface
								.existResourceArchive(resourceId)) {
							if (!ResourceDirectoryInterface
									.deleteResourceArchive(resourceId))
								logger.warn(String
										.format("Failed to delete archive for resource %s",
												resourceId));

						}
					if (successfullFileCollectionDelection)
						successFullDirectoryDeletion = ResourceDirectoryInterface
								.deleteResourceDirectory(resourceId);

					// if the directory has been successfully deleted or didn't
					// exist,
					// and if the database entry exists, clean the database
					// entry
					if ((successFullDirectoryDeletion || !existsResourceDirectory)
							&& entryExistsInDatabase) {
						try {
							// if there is an url
							if (StringUtils.isNotBlank(url)) {
								// but the resource is of type file
								if (ContentType.isFile(scormType)) {
									String errorReport = String
											.format("The resource %s to be deleted has scorm type %s but his entry in database contains the non blank url %s",
													resourceId, scormType, url);
									// we will only log a warning
									logger.warn(errorReport);
									warnings.append(errorReport);
								}
								// ask Solr to delete this url from index
								try {
									searchEngineQueryHandler
											.processDeleteQuery(SolrJSearchEngineQueryHandler
													.getDocumentIdentifier(
															resourceId, url));
								} catch (SearchEngineErrorException e) {
									logger.error(String
											.format("A search engine error occured while trying to remove url %s for resource %s from search engine index with message %s",
													url, resourceId,
													e.getMessage()));
								} catch (SearchEngineCommunicationException e) {
									logger.error(String
											.format("A communication problem with search engine occured while trying to remove url %s for resource %s from search engine index with message %s",
													url, resourceId,
													e.getMessage()));
								}
								searchEngineWaitingForCommit = true;
							}
							// now delete the resource from database
							resourceDataHandler.deleteResourceEntry(resourceId);
						} catch (InexistentResourceInDatabaseException e) {
							// the resource does not exist in database
							// do nothing. it has already been signaled and
							// this portion of code is logically inaccessible
						}
					}

					if (!successFullDirectoryDeletion) {
						String errorReport = String.format(
								"Failed to delete directory for resource %s",
								resourceId);
						warnings.append(errorReport);
						logger.error(errorReport);
						// directory exists but we were unable to delete it
						if (response == null)
							response = Response
									.status(Status.INTERNAL_SERVER_ERROR)
									.entity(warnings.toString())
									.type(MediaType.TEXT_PLAIN);
					}

				} else {// we did not find a directory on the file system for
					Object message = String
							.format("An attempt was made to delete the resource %s but no directory was found for it",
									resourceId);
					warnings.append(message);
					logger.error(message);
					// if we did'nt find an entry in database, neither a
					// directory, this resource has
					// never existed
					if (!entryExistsInDatabase)
						if (response == null)
							response = Response
									.status(Status.NOT_FOUND)
									.entity(rb
											.getResourceUnsuccessfulDestructionReport(
													realPath(context), uriInfo,
													apiscolInstanceName,
													resourceId,
													warnings.toString()));
				}
				if (searchEngineWaitingForCommit)
					try {
						searchEngineQueryHandler.processCommitQuery();
					} catch (SearchEngineErrorException e) {
						logger.error(String
								.format("A search engine error occured while trying to commit to search engine index with message %s",

								e.getMessage()));
					} catch (SearchEngineCommunicationException e) {
						logger.error(String
								.format("A communication problem with search engine occured while trying to commit to search engine index from search engine index with message %s",
										e.getMessage()));
					}
				String resourcePreviewDirectory = rb
						.getResourcePreviewDirectoryUri(uriInfo, resourceId);
				deleteResourcePreview(resourceId, resourcePreviewDirectory);
				if (response == null) {

					{
						response = Response
								.ok(rb.getResourceSuccessfulDestructionReport(
										realPath(context), uriInfo,
										apiscolInstanceName, resourceId,
										warnings.toString()), rb.getMediaType());
						deleteResourceDownloadableArchive(resourceId);
					}

				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for resource %s",
							resourceId));
		}

		return response.build();

	}

	private void deleteResourceDownloadableArchive(String resourceId) {
		if (!ResourceDirectoryInterface.deleteResourceArchive(resourceId))
			logger.error(String.format(
					"Unable to delete archive directory for resource %s",
					resourceId));

	}

	private void checkResidSyntax(String resourceId)
			throws IncorrectResourceKeySyntaxException {
		if (!ResourcesKeySyntax.resourceIdIsCorrect(resourceId))
			throw new IncorrectResourceKeySyntaxException(resourceId);

	}

	@GET
	@Path("/resource/{resid}/thumbs")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getThumbsSuggestions(
			@Context HttpServletRequest request,
			@PathParam(value = "resid") final String resourceId,
			@DefaultValue("64") @QueryParam(value = "min_dimensions_sum") final int minDimensionsSum,
			@QueryParam(value = "format") final String format)
			throws DBAccessException, InexistentResourceInDatabaseException,
			IncorrectResourceKeySyntaxException,
			ResourceDirectoryNotFoundException,
			UnknownMediaTypeForResponseException {

		checkResidSyntax(resourceId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		ThumbExtracter thumbsExtracter = ThumbExtracterFactory
				.getExtracter(resourceDataHandler
						.getScormTypeForResource(resourceId));
		Map<String, Point> thumbsUris = thumbsExtracter.getThumbsFromResource(
				resourceId, resourceDataHandler,
				ResourcesKeySyntax.removeSSL(uriInfo.getBaseUri().toString()),
				resourceDataHandler.getMainFileForResource(resourceId),
				minDimensionsSum);
		thumbsUris.putAll(thumbsExtracter.getThumbsFromPreview(resourceId,
				previewsRepoPath, uriInfo.getBaseUri().toString()));
		Object response = rb.getThumbListRepresentation(resourceId, thumbsUris,
				uriInfo, apiscolInstanceName, ResourceApi.editUri);
		return Response.ok(response, rb.getMediaType()).build();
	}

	public static void stopExecutors() {
		if (logger != null)
			logger.info("Thread executors are going to be stopped for Apiscol Content.");
		if (compressionExecutor != null)
			compressionExecutor.shutdown();
		if (previewMakerExecutor != null)
			previewMakerExecutor.shutdown();

	}

}
