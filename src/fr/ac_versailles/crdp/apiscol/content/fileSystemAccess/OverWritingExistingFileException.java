package fr.ac_versailles.crdp.apiscol.content.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class OverWritingExistingFileException extends ApiscolException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OverWritingExistingFileException(String resourceId, String fileName) {
		super(String.format("The provided file %s already exists in the resource %s, delete the old one before adding", resourceId, fileName));
	}

}
