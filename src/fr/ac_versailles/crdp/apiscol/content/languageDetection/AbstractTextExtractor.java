package fr.ac_versailles.crdp.apiscol.content.languageDetection;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractTextExtractor implements ITextExtrator {

	protected static Logger logger;

	protected void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

}
