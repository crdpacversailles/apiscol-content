package fr.ac_versailles.crdp.apiscol.content.thumbs;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class InvalidImageException extends ApiscolException {

	public InvalidImageException(String message) {
		super(message, false);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
