package fr.ac_versailles.crdp.apiscol.content.crawler;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineCommunicationException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineErrorException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SolrJSearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.UrlBadSyntaxException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.UrlParsingException;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class LinkRefresher implements Runnable {

	private final ISearchEngineQueryHandler searchEngineQueryHandler;
	private final LinkRefreshingHandler caller;
	private final IResourceDataHandler resourceDataHandler;
	private Logger logger;

	public enum Terminations {
		SUCCESSFULL, ERRORS, ABORTED, NONE
	}

	public LinkRefresher(ISearchEngineQueryHandler searchEngineQueryHandler,
			IResourceDataHandler resourceDataHandler,
			LinkRefreshingHandler caller) {
		this.searchEngineQueryHandler = searchEngineQueryHandler;
		this.resourceDataHandler = resourceDataHandler;
		this.caller = caller;
		createLogger();
	}

	@Override
	public void run() {
		Terminations termination = Terminations.SUCCESSFULL;
		Map<String, String> urlResources = null;
		try {
			urlResources = resourceDataHandler.getUrlResourcesList();
		} catch (DBAccessException e1) {
			logger.error(String
					.format("Connexion failure with database  while trying to refreshurls "));

			e1.printStackTrace();
			termination = Terminations.ABORTED;
			caller.notifyRefreshingProcessTermination(termination, 0);
			return;
		}
		int nbErrors = 0;
		Iterator<String> it = urlResources.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String url = urlResources.get(key);
			caller.setCurrentlyParsedUrl(url);
			logger.info("mise à jour de l'url " + url);
			try {
				searchEngineQueryHandler
						.processDeleteQuery(SolrJSearchEngineQueryHandler
								.getDocumentIdentifier(key, url));

			} catch (SearchEngineErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SearchEngineCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean deadURL = false;
			boolean connexionFailure = false;
			try {
				searchEngineQueryHandler.processAddQueryForUrl(
						SolrJSearchEngineQueryHandler.getDocumentIdentifier(
								key, url), url);
			} catch (UrlBadSyntaxException e) {
				logger.error(String.format(
						"The syntax of the url %s for resource %s is invalid.",
						url, key));
				deadURL = true;
				termination = Terminations.ERRORS;
				e.printStackTrace();
			} catch (UrlParsingException e) {
				logger.error(String.format(
						"The resource at url %s for resource %s is dead.", url,
						key));
				termination = Terminations.ERRORS;
				deadURL = true;
				e.printStackTrace();
			} catch (SearchEngineCommunicationException e) {
				logger.error(String
						.format("Communication problem with the search engine while trying to parse url %s for resource %s.",
								url, key));
				termination = Terminations.ERRORS;
				connexionFailure = true;
				e.printStackTrace();
			} catch (SearchEngineErrorException e) {
				logger.error(String
						.format("Search engine error while trying to parse url %s for resource %s.",
								url, key));
				termination = Terminations.ERRORS;
				connexionFailure = true;
				e.printStackTrace();
			}
			try {
				resourceDataHandler.markUrlAsDead(key, deadURL);
			} catch (InexistentResourceInDatabaseException e) {
				logger.error(String
						.format("Inexistent resource in database : %s while tryning to mark url : %s as dead : %s",
								key, url, deadURL));
				termination = Terminations.ERRORS;
				e.printStackTrace();
			} catch (DBAccessException e) {
				logger.error(String
						.format("Connexion failure with database  for resource : %s while trying to mark url : %s as dead : %s",
								key, url, deadURL));
				termination = Terminations.ERRORS;
				e.printStackTrace();
			}
			if (deadURL || connexionFailure)
				nbErrors++;
		}
		caller.notifyRefreshingProcessTermination(termination, nbErrors);
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());
	}

}
