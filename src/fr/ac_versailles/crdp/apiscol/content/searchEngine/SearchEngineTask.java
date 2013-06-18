package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fr.ac_versailles.crdp.apiscol.content.AsyncProcessTrackingObject;
import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.content.IRefreshProcess;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceSnapshotDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class SearchEngineTask implements IRefreshProcess {

	private final String resourceId;
	private Logger logger;
	private AsyncProcessTrackingObject trackingObject;
	private final IResourceDataHandler resourceDataHandler;
	private final ISearchEngineQueryHandler searchEngineQueryHandler;

	public SearchEngineTask(String resourceId,
			IResourceDataHandler resourceDataHandler,
			ISearchEngineQueryHandler searchEngineQueryHandler)
			throws IOException, ResourceDirectoryNotFoundException {
		this.resourceDataHandler = resourceDataHandler;
		this.searchEngineQueryHandler = searchEngineQueryHandler;
		createLogger();
		this.resourceId = resourceId;
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

	@Override
	public void run() {
		trackingObject.updateStateAndMessage(States.pending,
				"The resource is going to be indexed again.");
		String actualScormType = null;
		try {
			actualScormType = resourceDataHandler
					.getScormTypeForResource(resourceId);
		} catch (DBAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InexistentResourceInDatabaseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (ContentType.isLink(actualScormType)) {
			trackingObject
					.updateStateAndMessage(States.pending,
							"The resource has been found in database and identified as a remote resource.");
			String actualUrl = null;
			try {
				actualUrl = resourceDataHandler.getUrlForResource(resourceId);
				trackingObject
						.updateStateAndMessage(
								States.pending,
								"The resource has been found in database and identified as a remote resource with the following link : "
										+ actualUrl);
			} catch (DBAccessException e1) {
				e1.printStackTrace();
				trackingObject.updateStateAndMessage(States.aborted,
						"The database seems out of reach :" + e1.getMessage());
				return;

			} catch (InexistentResourceInDatabaseException e1) {
				e1.printStackTrace();
				trackingObject.updateStateAndMessage(
						States.aborted,
						"The resource does not exist in database :"
								+ e1.getMessage());
				return;
			}

			String documentIdentifier = SolrJSearchEngineQueryHandler
					.getDocumentIdentifier(resourceId, actualUrl);
			if (StringUtils.isNotBlank(actualUrl))
				try {

					searchEngineQueryHandler
							.processDeleteQuery(documentIdentifier);
					trackingObject.updateStateAndMessage(States.pending,
							"The resource has been deleted from Solr index.");
					try {
						searchEngineQueryHandler.processAddQueryForUrl(
								documentIdentifier, actualUrl);
						trackingObject
								.updateStateAndMessage(
										States.pending,
										"The remote  resource "
												+ actualUrl
												+ "has been added again to solr index with identifier : "
												+ documentIdentifier);
					} catch (UrlBadSyntaxException e) {
						trackingObject.updateStateAndMessage(
								States.aborted,
								"The url " + actualUrl
										+ " has a wrong syntax :"
										+ e.getMessage());
						return;
					} catch (UrlParsingException e) {
						trackingObject.updateStateAndMessage(
								States.aborted,
								"The url " + actualUrl
										+ " was impossible to parse :"
										+ e.getMessage());
						return;
					}
				} catch (SearchEngineErrorException e) {
					String message = String
							.format("A search engine error occured while trying to remove url %s for resource %s from search engine index with message %s",
									actualUrl, resourceId, e.getMessage());
					logger.error(message);
					trackingObject.updateStateAndMessage(States.aborted,
							message);
					return;
				} catch (SearchEngineCommunicationException e) {
					String message = String
							.format("A communication problem with search engine occured while trying to remove url %s for resource %s from search engine index with message %s",
									actualUrl, resourceId, e.getMessage());
					logger.error(message);
					trackingObject.updateStateAndMessage(States.aborted,
							message);
					return;
				}

			try {
				searchEngineQueryHandler.processCommitQuery();
				trackingObject
						.updateStateAndMessage(
								States.done,
								"The remote  resource "
										+ actualUrl
										+ "has been successfully added again to solr index with identifier : "
										+ documentIdentifier
										+ " and changes have been commited.");

			} catch (SearchEngineErrorException e) {
				String message = String
						.format("A search engine error occured while trying to commit search engine for resource index with message %s",
								resourceId, e.getMessage());
				logger.error(message);
				trackingObject.updateStateAndMessage(States.aborted, message);
			} catch (SearchEngineCommunicationException e) {
				String message = String
						.format("A communication problem with search engine occured while trying to commit search engine for resource index with message %s",
								resourceId, e.getMessage());
				logger.error(message);
				trackingObject.updateStateAndMessage(States.aborted, message);
				return;
			}
		} else {
			trackingObject
					.updateStateAndMessage(States.pending,
							"The resource has been found in database and identified as a local resource");
			ArrayList<String> list = null;
			try {
				list = ResourceDirectoryInterface.getFileNamesList(resourceId);
			} catch (ResourceDirectoryNotFoundException e1) {
				String message = "Impossible to read file  list for resource "
						+ resourceId + "with message " + e1.getMessage();
				trackingObject.updateStateAndMessage(States.aborted, message);
				e1.printStackTrace();
				return;
			}
			if (list.size() == 0) {
				trackingObject.updateStateAndMessage(States.aborted,
						"There are no files to index");
				return;
			}
			Iterator<String> it = list.iterator();
			String fileName = null;
			StringBuilder fileList = new StringBuilder();
			while (it.hasNext()) {
				fileName = (String) it.next();
				fileList.append(fileName);
				if (it.hasNext())
					fileList.append(", ");
				try {
					searchEngineQueryHandler
							.processDeleteQuery(SolrJSearchEngineQueryHandler
									.getDocumentIdentifier(resourceId, fileName));

					searchEngineQueryHandler
							.processAddQueryForFile(
									SolrJSearchEngineQueryHandler
											.getDocumentIdentifier(resourceId,
													fileName),
									ResourceDirectoryInterface.getFilePath(
											resourceId, fileName));
					searchEngineQueryHandler.processCommitQuery();
					trackingObject
							.updateStateAndMessage(
									States.pending,
									"The following files have been successfully removed from Solr index and added gain : "
											+ fileList.toString());
					trackingObject
							.updateStateAndMessage(
									States.done,
									"The following files have been successfully removed from Solr index and added gain : "
											+ fileList.toString()
											+ "for resource " + resourceId);
				} catch (SearchEngineErrorException e) {
					String message = String
							.format("A search engine error occured while trying to add file %s for resource %s from search engine index with message %s",
									fileName, resourceId, e.getMessage());
					logger.error(message);
					trackingObject.updateStateAndMessage(States.aborted,
							message);
					return;
				} catch (SearchEngineCommunicationException e) {
					String message = String
							.format("A communication problem with search engine occured while trying to add file %s for resource %s from search engine index with message %s",
									fileName, resourceId, e.getMessage());
					logger.error(message);
					trackingObject.updateStateAndMessage(States.aborted,
							message);
					return;
				}
			}
		}

	}

	@Override
	public void setTrackingObject(AsyncProcessTrackingObject trackingObject) {
		this.trackingObject = trackingObject;

	}

}
