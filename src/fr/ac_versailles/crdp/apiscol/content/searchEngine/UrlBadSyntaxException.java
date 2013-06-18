package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class UrlBadSyntaxException extends ApiscolException {

	public UrlBadSyntaxException(String url) {
		super(String.format("The url %s is not correctly formed.", url));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
