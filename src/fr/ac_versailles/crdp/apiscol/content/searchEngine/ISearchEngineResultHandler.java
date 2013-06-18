package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISearchEngineResultHandler {

	public abstract Map<String, String> getResultTypesById();

	public abstract Map<String, String> getResultScoresById();

	public abstract Map<String, List<String>> getResultSnippetsById();

	public abstract Set<String> getResultsIds();

	public abstract Map<String, List<String>> getWordSuggestionsByQueryTerms();

	public abstract List<String> getQuerySuggestions();

	public abstract void parse(Object searchResult);

}