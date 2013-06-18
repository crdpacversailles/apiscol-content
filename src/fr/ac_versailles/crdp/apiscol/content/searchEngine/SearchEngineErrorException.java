package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class SearchEngineErrorException extends ApiscolException {

	public SearchEngineErrorException(String message) {
		super(message, true);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
