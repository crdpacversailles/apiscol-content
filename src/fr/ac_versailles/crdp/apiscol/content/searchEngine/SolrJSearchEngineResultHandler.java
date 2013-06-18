package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrJSearchEngineResultHandler implements
		ISearchEngineResultHandler {
	private Set<String> resultsIds;
	private Map<String, String> resultTypesById;
	private Map<String, String> resultScoresById;
	private Map<String, List<String>> resultSnippetsById;
	private Map<String, List<String>> wordSuggestionsByQueryTerms;
	private List<String> querySuggestions;

	public SolrJSearchEngineResultHandler() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getResultTypesById()
	 */
	@Override
	public Map<String, String> getResultTypesById() {
		return resultTypesById;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getResultScoresById()
	 */
	@Override
	public Map<String, String> getResultScoresById() {
		return resultScoresById;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getResultSnippetsById()
	 */
	@Override
	public Map<String, List<String>> getResultSnippetsById() {
		return resultSnippetsById;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getResultsIds()
	 */
	@Override
	public Set<String> getResultsIds() {
		return resultsIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getWordSuggestionsByQueryTerms()
	 */
	@Override
	public Map<String, List<String>> getWordSuggestionsByQueryTerms() {
		return wordSuggestionsByQueryTerms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler
	 * #getQuerySuggestions()
	 */
	@Override
	public List<String> getQuerySuggestions() {
		return querySuggestions;
	}

	@Override
	public void parse(Object searchResult) {
		QueryResponse response = (QueryResponse) searchResult;
		SolrDocumentList documents = response.getResults();
		Map<String, Map<String, List<String>>> highlights = response
				.getHighlighting();
		resultsIds = new HashSet<String>();
		resultScoresById = new HashMap<String, String>();
		resultSnippetsById = new HashMap<String, List<String>>();
		resultTypesById = new HashMap<String, String>();
		querySuggestions = new ArrayList<String>();
		wordSuggestionsByQueryTerms = new HashMap<String, List<String>>();
		if (documents != null) {
			Iterator<SolrDocument> it = documents.iterator();
			while (it.hasNext()) {
				SolrDocument solrDocument = (SolrDocument) it.next();

				String id = (String) solrDocument.getFieldValue("id");
				resultsIds.add(id);
				//TODO handle case where these fields are null : throw and map exception
				resultTypesById.put(id,
						solrDocument.getFieldValue("content_type").toString());
				resultScoresById.put(id,
						"" + solrDocument.getFieldValue("score"));
				resultSnippetsById.put(id, new ArrayList<String>());
			}
			if (highlights != null) {
				Iterator<String> it2 = highlights.keySet().iterator();
				while (it2.hasNext()) {
					String id = (String) it2.next();
					Map<String, List<String>> highlight = highlights.get(id);
					if (highlight.keySet().contains("text")) {
						List<String> snippets = highlight.get("text");
						resultSnippetsById.put(id, snippets);
					}
				}
			}
		}

		SpellCheckResponse spellchecks = response.getSpellCheckResponse();
		if (spellchecks != null) {
			Map<String, Suggestion> suggestionMap = spellchecks
					.getSuggestionMap();
			if (suggestionMap != null) {
				Iterator<String> it3 = suggestionMap.keySet().iterator();
				while (it3.hasNext()) {
					String word = (String) it3.next();
					Suggestion suggestion = suggestionMap.get(word);
					List<String> alternatives = suggestion.getAlternatives();
					wordSuggestionsByQueryTerms.put(word, alternatives);

				}
			}
			List<Collation> collatedResults = spellchecks.getCollatedResults();
			if (collatedResults != null) {
				Iterator<Collation> it4 = collatedResults.iterator();
				while (it4.hasNext()) {
					SpellCheckResponse.Collation collation = (SpellCheckResponse.Collation) it4
							.next();
					querySuggestions.add(collation.getCollationQueryString());
				}
			}
		}

	}
}
