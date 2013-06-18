package fr.ac_versailles.crdp.apiscol.content;

import fr.ac_versailles.crdp.apiscol.ApiscolException;


public class IllegalResourceTypeChangeException extends ApiscolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IllegalResourceTypeChangeException(String type, String resourceId) {
		super(String.format(
				"Client trying to change resource type from %s to url for resource %s, but the resources is not void, delete files before changing type.",
				type, resourceId));
	}

}
