package fr.ac_versailles.crdp.apiscol.content.languageDetection;

import java.io.File;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.parser.AutoDetectParser;

public class TikaTextExtractor extends AbstractTextExtractor {

	private static Tika tika;	

	@Override
	public String extractText(File file) {
		createLogger();
		createTika();
		try {
			return tika.parseToString(file);
		} catch (Exception e) {
			logger.error("Tika has thrown an exception while trying to extract "
					+ file.getAbsolutePath()
					+ "with message : "
					+ e.getMessage());
			return StringUtils.EMPTY;
		}
	}

	private void createTika() {
		if (tika != null)
			return;
		tika = new Tika(new DefaultDetector(), new AutoDetectParser());

	}

	@Override
	public String extractText(URL url) {
		createLogger();
		createTika();
		try {
			return tika.parseToString(url);
		} catch (Exception e) {
			logger.error("Tika has thrown an exception while trying to extract "
					+ url.toString() + "with message : " + e.getMessage());
			return StringUtils.EMPTY;
		}
	}
}
