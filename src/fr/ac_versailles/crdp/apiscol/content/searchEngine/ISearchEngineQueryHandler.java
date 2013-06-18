package fr.ac_versailles.crdp.apiscol.content.searchEngine;

public interface ISearchEngineQueryHandler {

	public abstract Object processSearchQuery(String keywords, float fuzzy)
			throws SearchEngineErrorException;

	public abstract Object processSpellcheckQuery(String query)
			throws SearchEngineErrorException;

	public abstract String processAddQueryForFile(String documentIdentifier,
			String filePath) throws SearchEngineErrorException,
			SearchEngineCommunicationException;

	public abstract String processAddQueryForUrl(String documentIdentifier,
			String url) throws UrlBadSyntaxException, UrlParsingException,
			SearchEngineCommunicationException, SearchEngineErrorException;

	public abstract String processCommitQuery()
			throws SearchEngineErrorException,
			SearchEngineCommunicationException;

	public abstract String processDeleteQuery(String documentIdentifier)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException;

	public abstract void processOptimizationQuery()
			throws SearchEngineErrorException,
			SearchEngineCommunicationException;

	public abstract void deleteIndex() throws SearchEngineErrorException, SearchEngineCommunicationException;

}