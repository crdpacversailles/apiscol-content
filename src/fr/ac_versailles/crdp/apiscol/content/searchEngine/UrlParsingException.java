package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class UrlParsingException extends ApiscolException {

	public UrlParsingException(String url, String error) {
		super(String.format("The search engine cannot parse the provided url %s and aborts with the message %s.", url, error));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
