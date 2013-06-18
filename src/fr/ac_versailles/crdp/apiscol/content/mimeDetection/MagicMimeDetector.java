package fr.ac_versailles.crdp.apiscol.content.mimeDetection;

import java.io.File;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

public class MagicMimeDetector extends AbstractMimeDetector {

	@Override
	public String detectType(File file) {
		createLogger();
		String mimeType;
		if (file.getName().endsWith("mp4"))
			return "video/mp4";
		if (file.getName().endsWith("flv"))
			return "video/flv";
		try {
			mimeType = Magic.getMagicMatch(file, false).getMimeType();
		} catch (MagicParseException e) {
			logger.warn(String
					.format("Impossible to parse magic bytes for file %s with message %s",
							file.getName(), e.getMessage()));
			e.printStackTrace();
			return null;
		} catch (MagicMatchNotFoundException e) {
			logger.warn(String.format(
					"No magic match found for file %s with message %s",
					file.getName(), e.getMessage()));
			e.printStackTrace();
			return null;
		} catch (MagicException e) {
			logger.warn(String.format(
					"Mime magic error while handling file %s with message %s",
					file.getName(), e.getMessage()));
			e.printStackTrace();
			return null;
		}
		if (mimeType.toString().equals("application/zip")
				&& file.getName().endsWith("ubz"))
			return "application/ubz";
		if (mimeType.toString().equals("application/zip")
				&& file.getName().endsWith("xia"))
			return "application/xia";
		return mimeType.toString();
	}

}
