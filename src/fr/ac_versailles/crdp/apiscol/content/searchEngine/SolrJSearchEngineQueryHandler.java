package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class SolrJSearchEngineQueryHandler implements ISearchEngineQueryHandler {

	private final String solrSearchPath;
	private final String solrSuggestPath;
	private final String solrUpdatePath;
	private final String solrExtractPath;
	private HttpSolrServer solr;
	private static Logger logger;

	public SolrJSearchEngineQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath,
			String solrExtractPath, String solrSuggestPath) {
		this.solrSearchPath = solrSearchPath;
		this.solrUpdatePath = solrUpdatePath;
		this.solrExtractPath = solrExtractPath;
		this.solrSuggestPath = solrSuggestPath;
		solr = new HttpSolrServer(solrAddress);
		solr.setParser(new XMLResponseParser());
	}

	@Override
	public Object processSearchQuery(String keywords, float fuzzy)
			throws SearchEngineErrorException {
		createLogger();
		SolrQuery parameters = new SolrQuery();
		parameters.set("spellcheck.q", keywords);
		if (fuzzy > 0) {
			String[] words = keywords.split("\\s+");
			for (int i = 0; i < words.length; i++) {
				words[i] += "~" + fuzzy;
			}
			keywords = StringUtils.join(words, " ");
		}
		parameters.set("q", keywords);
		parameters.set("qt", solrSearchPath);
		QueryResponse response = null;
		try {
			response = solr.query(parameters);
		} catch (SolrException e) {
			String error = String
					.format("Solr has thrown a runtime exception when he was asked to search keywords  %s whith the message %s",
							keywords, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to search keywords  %s whith the message %s",
							keywords, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		}
		return response;
	}

	@Override
	public Object processSpellcheckQuery(String keywords)
			throws SearchEngineErrorException {
		createLogger();
		SolrQuery parameters = new SolrQuery();
		parameters.set("spellcheck.q", keywords);
		parameters.set("qt", solrSuggestPath);
		QueryResponse response = null;
		try {
			response = solr.query(parameters);
		} catch (SolrException e) {
			String error = String
					.format("Solr has thrown a runtime exception when he was asked to search keywords  for completion  %s whith the message %s",
							keywords, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to search keywords  for completion  %s whith the message %s",
							keywords, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		}
		return response;
	}

	@Override
	public String processAddQueryForFile(String documentIdentifier,
			String filePath) throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(
				String.format("%s%s", solrUpdatePath, solrExtractPath));
		createLogger();
		File file = new File(filePath);
		try {

			req.addFile(file, "application/octet-stream");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// req.setParam(ExtractingParam.EXTRACT_ONLY, "true");
		req.setParam("fmap.content", "text");
		req.setParam("literal.id", documentIdentifier);
		req.setParam("resource.name", file.getName());
		NamedList<Object> result = null;
		try {
			result = solr.request(req);
		} catch (SolrException e) {
			String error = String
					.format("Solr has thrown a runtime exception when he was asked to index file %s whith the message %s",
							filePath, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to index file %s whith the message %s",
							filePath, e.getMessage());

			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (IOException e) {
			String error = String
					.format("There was a connexion problem with solr when he was asked to index file %s whith the message %s",
							filePath, e.getMessage());
			logger.error(error);
			throw new SearchEngineCommunicationException(error);
		}

		logger.info(String.format(
				"Query to solr : add the file %s with id : %s", filePath,
				documentIdentifier));

		return "";
	}

	@Override
	public String processAddQueryForUrl(String documentIdentifier, String url)
			throws UrlBadSyntaxException, UrlParsingException,
			SearchEngineCommunicationException, SearchEngineErrorException {
		ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(
				String.format("%s%s", solrUpdatePath, solrExtractPath));
		createLogger();
		try {
			req.addContentStream(new ContentStreamBase.URLStream(new URL(url)));
		} catch (MalformedURLException e1) {
			throw new UrlBadSyntaxException(url);
		}
		req.setParam("fmap.content", "text");
		req.setParam("literal.id", documentIdentifier);

		logger.info(String.format(
				"Query to solr : add the url %s with id : %s xxx", url,
				documentIdentifier));
		NamedList<Object> result = null;
		try {
			result = solr.request(req);

		} catch (SolrException e) {
			String error = String
					.format("Solr has thrown a runtime exception when he was asked to parse url %s, whith the message  %s",
							url, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (SolrServerException e) {
			throw new UrlParsingException(url, e.getMessage());
		} catch (IOException e) {
			throw new SearchEngineCommunicationException(
					"Unable to communicate with the search engine. "
							+ e.getMessage());
		} finally {
			logger.info("Result: " + result);
		}

		return "";
	}

	@Override
	public String processCommitQuery() throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		try {
			solr.commit();
		} catch (SolrException e) {
			String error = String
					.format("Solr has thrown a runtime exception when he was asked to commit, whith the message  %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to commit, whith the message  %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (IOException e) {
			String error = String
					.format("There was a connexion problem with solr when he was asked commit, whith the message %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineCommunicationException(error);
		}
		// TODO what's that
		return "";
	}

	@Override
	public String processDeleteQuery(String documentIdentifier)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		createLogger();
		logger.info(String.format("Query to solr : delete with id : %s",
				documentIdentifier));
		UpdateResponse result = null;
		try {
			result = solr.deleteById(documentIdentifier);
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to delete docuument %s whith the message %s",
							documentIdentifier, e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (IOException e) {
			String error = String
					.format("There was a connexion problem with solr when he was asked to index file %s whith the message %s",
							documentIdentifier, e.getMessage());
			logger.error(error);
			throw new SearchEngineCommunicationException(error);
		}
		return result.toString();
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(SolrJSearchEngineQueryHandler.class
							.getCanonicalName());

	}

	@Override
	public void processOptimizationQuery() throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		try {
			solr.optimize();
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to optimize index whith the message %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (IOException e) {
			String error = String
					.format("There was a connexion problem with solr when he was asked to optimize index whith the message %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineCommunicationException(error);
		}

	}

	public static String getDocumentIdentifier(String resourceId,
			String fileName) {
		// TODO ne pas mettre static, passer dans l'interface
		return String.format("%s@%s", resourceId, fileName);
	}

	@Override
	public void deleteIndex() throws SearchEngineErrorException,
			SearchEngineCommunicationException {
		try {
			solr.deleteByQuery("*:*");
			solr.commit();
		} catch (SolrServerException e) {
			String error = String
					.format("Solr has thrown an exception when he was asked to delete the index whith the message %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineErrorException(error);
		} catch (IOException e) {
			String error = String
					.format("There was a connexion problem with solr when he was asked to delete the index whith the message %s",
							e.getMessage());
			logger.error(error);
			throw new SearchEngineCommunicationException(error);
		}

	}

}
