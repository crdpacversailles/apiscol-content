package fr.ac_versailles.crdp.apiscol.content.mimeDetection;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.parser.AutoDetectParser;

public class TikaMimeDetector extends AbstractMimeDetector {

	private static Tika tika;

	@Override
	public String detectType(File file) {
		createLogger();
		createTika();
		Reader reader = null;
		try {
			reader = tika.parse(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		char[] buf = new char[1024];
		int numRead = 0;
		try {
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				buf = new char[1024];
			}
		} catch (IOException e1) {
			logger.error("Tika was not able to Parse : "
					+ file.getAbsolutePath()
					+ ". unsolved exception : java.lang.NoClassDefFoundError: org/apache/tika/parser/txt/UniversalEncodingListener");
			// e1.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String mimeType = null;
		try {
			mimeType = tika.detect(file);
			logger.info("Tika has detected the following mime type :"
					+ mimeType + " for the file " + file.getAbsolutePath());
		} catch (IOException e) {
			logger.error("Tika was not able to Parse : "
					+ file.getAbsolutePath());
			e.printStackTrace();
			return "";
		}

		return mimeType.toString();
	}

	private void createTika() {
		if (tika != null)
			return;
		tika = new Tika(new DefaultDetector(), new AutoDetectParser());

	}
}
