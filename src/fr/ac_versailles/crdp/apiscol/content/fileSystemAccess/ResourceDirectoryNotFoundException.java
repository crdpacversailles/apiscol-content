package fr.ac_versailles.crdp.apiscol.content.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class ResourceDirectoryNotFoundException extends ApiscolException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ResourceDirectoryNotFoundException(String resourceId) {
		super(
				String.format(
						"The resource directory %s was not found, impossible to achieve the request.",
						resourceId));
	}

}
