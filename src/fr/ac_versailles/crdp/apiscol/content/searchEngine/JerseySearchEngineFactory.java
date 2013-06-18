package fr.ac_versailles.crdp.apiscol.content.searchEngine;

public class JerseySearchEngineFactory implements ISearchEngineFactory {

	@Override
	public ISearchEngineQueryHandler getQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath, String solrExtractPath, String solrSuggestPath) {
		return new JerseySearchEngineQueryHandler(solrAddress, solrSearchPath, solrUpdatePath, solrExtractPath);
	}

	@Override
	public ISearchEngineResultHandler getResultHandler() {
		return new JerseySearchEngineResultHandler();
	}

}
