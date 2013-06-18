package fr.ac_versailles.crdp.apiscol.content.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class MissingIncomingFileException extends ApiscolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public MissingIncomingFileException(String resourceId, String fileName) {
		super(String.format(
				"Client wants to register file %s for the resource %s, but the file has not been properly transmitted.",
				resourceId, fileName));
	}

}
