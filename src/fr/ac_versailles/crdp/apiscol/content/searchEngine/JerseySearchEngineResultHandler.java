package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JerseySearchEngineResultHandler extends DefaultHandler implements ISearchEngineResultHandler {
	private Set<String> resultsIds;
	private Map<String, String> resultTypesById;
	private Map<String, String> resultScoresById;
	private Map<String, List<String>> resultSnippetsById;
	private Map<String, List<String>> wordSuggestionsByQueryTerms;
	private List<String> querySuggestions;
	private boolean inResult, inDoc, inId, inScore, inType, inHighlighting,
			inHighlightingResult, inSnippet;
	private StringBuffer buffer;
	private String currentId;
	private String currentScore;
	private String currentType;
	private List<String> currentSnippet;

	private boolean inSuggestions;
	private boolean inTerm;
	private String currentMissCheckedTerm;
	private List<String> currentWordSuggestions;
	private boolean inWord;
	private boolean inSuggestion;
	private boolean inCollation;
	private boolean inMisspellingsAndCorrections;
	private boolean inCollationQuery;

	public JerseySearchEngineResultHandler() {

	}

	@Override
	public void startDocument() throws SAXException {
		createCollection();
	}

	private void createCollection() {
		resultTypesById = new HashMap<String, String>();
		resultScoresById = new HashMap<String, String>();
		resultSnippetsById = new HashMap<String, List<String>>();
		resultsIds = new HashSet<String>();
		wordSuggestionsByQueryTerms = new HashMap<String, List<String>>();
		querySuggestions = new ArrayList<String>();
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName == "result") {
			inResult = true;
		} else if (inResult && qName.equals("doc")) {
			inDoc = true;
			currentId = null;
			currentScore = null;
			currentType = null;
		} else if (inDoc && qName.equals("str")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("id")) {
			buffer = new StringBuffer();
			inId = true;
		} else if (inDoc && qName.equals("float")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("score")) {
			buffer = new StringBuffer();
			inScore = true;
		} else if (inDoc && qName.equals("arr")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("content_type")
				&& currentType == null) {
			inType = true;
		} else if (inDoc && inType && qName.equals("str")) {
			buffer = new StringBuffer();
		} else if (qName.equals("lst") && attributes.getValue("name") != null
				&& attributes.getValue("name").equals("highlighting")) {
			inHighlighting = true;
		} else if (inHighlighting && qName.equals("lst")
				&& attributes.getValue("name") != null) {
			currentId = attributes.getValue("name");
			currentSnippet = new ArrayList<String>();
			inHighlightingResult = true;
		} else if (inHighlightingResult && qName.equals("str")) {
			inSnippet = true;
			buffer = new StringBuffer();
		} else if (qName.equals("lst") && attributes.getValue("name") != null
				&& attributes.getValue("name").equals("suggestions")) {
			inSuggestions = true;
			buffer = new StringBuffer();
		} else if (inSuggestions
				&& !inTerm
				&& qName.equals("lst")
				&& attributes.getValue("name") != null
				&& !attributes.getValue("name").equals("collation")
				&& !attributes.getValue("name").equals(
						"misspellingsAndCorrections")) {
			currentMissCheckedTerm = attributes.getValue("name");
			currentWordSuggestions = new ArrayList<String>();
			inTerm = true;
		} else if (inTerm && qName.equals("arr")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("suggestion")) {
			inSuggestion = true;
		} else if (inSuggestion && qName.equals("str")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("word")) {
			buffer = new StringBuffer();
			inWord = true;
		} else if (inSuggestions && qName.equals("lst")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("collation")) {

			inCollation = true;
		} else if (inCollation && qName.equals("str")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals("collationQuery")) {
			buffer = new StringBuffer();
			inCollationQuery = true;
		} else if (inCollation
				&& qName.equals("lst")
				&& attributes.getValue("name") != null
				&& attributes.getValue("name").equals(
						"misspellingsAndCorrections")) {
			inMisspellingsAndCorrections = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName == "result") {
			inResult = false;
		} else if (inResult && qName.equals("doc")) {
			resultTypesById.put(currentId, currentType);
			resultScoresById.put(currentId, currentScore);

			inDoc = false;
		} else if (inResult && inDoc && inId && qName.equals("str")) {
			currentId = buffer.toString();
			if (!resultsIds.contains(currentId))
				resultsIds.add(currentId);
			buffer = null;
			inId = false;
		} else if (inResult && inDoc && inScore && qName.equals("float")) {
			currentScore = buffer.toString();
			buffer = null;
			inScore = false;
		} else if (inResult && inDoc && inType && qName.equals("arr")) {
			inScore = false;
		} else if (inResult && inDoc && inType && qName.equals("str")) {
			currentType = buffer.toString();
			buffer = null;
		} else if (inHighlighting && !inHighlightingResult
				&& qName.equals("lst")) {
			inHighlighting = false;
		} else if (inHighlightingResult && qName.equals("lst")) {
			inHighlightingResult = false;
			resultSnippetsById.put(currentId, currentSnippet);
			currentSnippet = null;
		} else if (inSnippet && qName.equals("str")) {
			inSnippet = false;
			String newSnippet = buffer.toString();
			currentSnippet.add(newSnippet);
		} else if (inSuggestions && !inTerm && qName.equals("lst")) {
			inSuggestions = false;
		} else if (inTerm && !inSuggestion && qName.equals("lst")) {
			wordSuggestionsByQueryTerms.put(currentMissCheckedTerm,
					currentWordSuggestions);
			currentMissCheckedTerm = null;
			currentWordSuggestions = null;
			inTerm = false;
		} else if (inSuggestion && !inWord && qName.equals("arr")) {
			inSuggestion = false;
		} else if (inWord && qName.equals("str")) {
			currentWordSuggestions.add(buffer.toString());
			buffer = null;
			inWord = false;
		} else if (inCollationQuery && qName.equals("str")) {
			querySuggestions.add(buffer.toString());
			buffer = null;
			inCollationQuery = false;
		} else if (inCollation && !inMisspellingsAndCorrections
				&& qName.equals("lst")) {
			inCollation = false;
		} else if (inMisspellingsAndCorrections && qName.equals("lst")) {
			inMisspellingsAndCorrections = false;
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String lecture = new String(ch, start, length);
		if (buffer != null)
			buffer.append(lecture);
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getResultTypesById()
	 */
	@Override
	public Map<String, String> getResultTypesById() {
		return resultTypesById;
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getResultScoresById()
	 */
	@Override
	public Map<String, String> getResultScoresById() {
		return resultScoresById;
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getResultSnippetsById()
	 */
	@Override
	public Map<String, List<String>> getResultSnippetsById() {
		return resultSnippetsById;
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getResultsIds()
	 */
	@Override
	public Set<String> getResultsIds() {
		return resultsIds;
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getWordSuggestionsByQueryTerms()
	 */
	@Override
	public Map<String, List<String>> getWordSuggestionsByQueryTerms() {
		return wordSuggestionsByQueryTerms;
	}

	/* (non-Javadoc)
	 * @see fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler#getQuerySuggestions()
	 */
	@Override
	public List<String> getQuerySuggestions() {
		return querySuggestions;
	}

	@Override
	public void parse(Object searchResult) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser;
		try {
			saxParser = factory.newSAXParser();
			saxParser.parse(new InputSource(new StringReader((String) searchResult)), this);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
