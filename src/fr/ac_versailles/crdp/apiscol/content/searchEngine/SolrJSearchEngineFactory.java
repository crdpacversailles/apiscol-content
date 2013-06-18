package fr.ac_versailles.crdp.apiscol.content.searchEngine;

public class SolrJSearchEngineFactory implements ISearchEngineFactory {

	@Override
	public ISearchEngineQueryHandler getQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath,
			String solrExtractPath, String solrSuggestPath) {
		return new SolrJSearchEngineQueryHandler(solrAddress, solrSearchPath,
				solrUpdatePath, solrExtractPath, solrSuggestPath);
	}

	@Override
	public ISearchEngineResultHandler getResultHandler() {
		return new SolrJSearchEngineResultHandler();
	}

}
