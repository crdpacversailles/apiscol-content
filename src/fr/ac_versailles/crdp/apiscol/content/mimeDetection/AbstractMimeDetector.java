package fr.ac_versailles.crdp.apiscol.content.mimeDetection;

import java.io.File;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractMimeDetector implements IMimeTypeDetector {

	protected static Logger logger;

	@Override
	public abstract String detectType(File mainFile);

	protected void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

}
