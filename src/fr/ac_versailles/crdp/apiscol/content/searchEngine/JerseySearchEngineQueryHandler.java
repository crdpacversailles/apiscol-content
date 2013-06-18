package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class JerseySearchEngineQueryHandler implements
		ISearchEngineQueryHandler {

	private static Logger logger;
	private static Client client;
	private static WebResource solrWebServiceUpdateResource;
	private static WebResource solrWebServiceQueryResource;
	private static WebResource solrWebServiceExtractResource;

	public JerseySearchEngineQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath, String solrExtractPath) {
		client = Client.create();
		solrWebServiceUpdateResource = client.resource(String.format("%s%s",
				solrAddress, solrUpdatePath));
		solrWebServiceQueryResource = client.resource(String.format("%s%s",
				solrAddress, solrSearchPath));
		solrWebServiceExtractResource = client.resource(String.format("%s%s%s",
				solrAddress, solrUpdatePath, solrExtractPath));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler
	 * #processSearchQuery(java.lang.String)
	 */
	@Override
	public String processSearchQuery(String keywords, float fuzzy) {
		createLogger();
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("q", keywords);
		try {
			return callRestfulWebService(solrWebServiceQueryResource,
					queryParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler
	 * #processAddQueryForFile(java.lang.String, java.lang.String)
	 */
	@Override
	public String processAddQueryForFile(String documentIdentifier,
			String filePath) {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("fmap.content", "text");
		queryParams.add("literal.id", documentIdentifier);
		queryParams.add("stream.file", filePath);
		createLogger();
		logger.info(String.format(
				"Query to solr : add the file %s with id : %s", filePath,
				documentIdentifier));
		try {
			return callRestfulWebService(solrWebServiceExtractResource,
					queryParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO implémenter un retour
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler
	 * #processAddQueryForUrl(java.lang.String, java.lang.String)
	 */
	@Override
	public String processAddQueryForUrl(String documentIdentifier, String url) {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("fmap.content", "text");
		queryParams.add("literal.id", documentIdentifier);
		queryParams.add("stream.url", url);
		createLogger();
		logger.info(String.format(
				"Query to solr : add the url %s with id : %s", url,
				documentIdentifier));
		try {
			return callRestfulWebService(solrWebServiceExtractResource,
					queryParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO implémenter un retour
		return "";
	}

	private void createLogger() {
		if (logger == null)
			logger  = LogUtility
					.createLogger(JerseySearchEngineQueryHandler.class
							.getCanonicalName());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler
	 * #processCommitQuery()
	 */
	@Override
	public String processCommitQuery() {
		createLogger();
		logger.info("Commit requested");
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("commit", "true");
		try {
			return callRestfulWebService(solrWebServiceUpdateResource,
					queryParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	private String callRestfulWebService(WebResource solrWebServiceResource,
			MultivaluedMap<String, String> queryParams) throws Exception {
		ClientResponse response = solrWebServiceResource
				.queryParams(queryParams)
				.accept(MediaType.APPLICATION_XML_TYPE)
				.get(ClientResponse.class);
		return response.getEntity(String.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler
	 * #processDeleteQuery(java.lang.String)
	 */
	@Override
	public String processDeleteQuery(String documentIdentifier) {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		String deleteRequest = String.format("<delete><id>%s</id></delete>",
				documentIdentifier);
		queryParams.add("stream.body", deleteRequest);

		createLogger();
		logger.info(String.format("Query to solr : delete with id : %s",
				documentIdentifier));
		try {
			return callRestfulWebService(solrWebServiceUpdateResource,
					queryParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

	}

	@Override
	public Object processSpellcheckQuery(String query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processOptimizationQuery() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteIndex() throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		// TODO Auto-generated method stub

	}

}
