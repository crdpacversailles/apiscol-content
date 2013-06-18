package fr.ac_versailles.crdp.apiscol.content.previews;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class PreviewMakersFactory {
	static String[] pdf = { "application/pdf" };
	static String[] officedocs = {
			"application/msword",
			"application/vnd.ms-excel",
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.oasis.opendocument.text",
			"application/rtf" };
	static String[] images = { "image/tiff", "image/jpeg", "image/png" };
	static String[] videos = { "video/x-ms-wmv", "video/x-m4v", "video/flv", "video/x-flv",
			"video/ogg", "video/avi" };

	public enum MimeTypeGroups {
		PDF(pdf), OFFICE_DOCUMENTS(officedocs), IMAGES(images), VIDEOS(videos);

		private String[] types;

		private MimeTypeGroups(String[] types) {
			this.types = types;
		}

		public List<String> list() {
			return Arrays.asList(types);
		}

	}

	private static Logger logger;

	private static void createLogger() {
		if (logger == null)
			logger = LogUtility.createLogger(PreviewMakersFactory.class
					.getCanonicalName());

	}

	public static IPreviewMaker getPreviewMaker(String mimeType,
			String resourceId, String previewsRepoPath, String entryPoint,
			boolean isRemote, String realPath, String previewUri) {
		if (logger == null)
			createLogger();
		logger.info("Askin preview maker for mime type : " + mimeType);
		if (isRemote)
			return new RemoteResourcePreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (StringUtils.equals(mimeType, "application/x-uniboard+zip"))
			return new UniboardPreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (StringUtils.equals(mimeType, "application/x-shockwave-flash"))
			return new FlashPreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (StringUtils.equals(mimeType, "image/jpeg")
				|| StringUtils.equals(mimeType, "image/png")
				|| StringUtils.equals(mimeType, "image/gif")
				|| StringUtils.equals(mimeType, "image/bmp")
				|| StringUtils.equals(mimeType, "image/tiff")
				|| StringUtils.equals(mimeType, "image/svg"))
			return new ImagePreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (MimeTypeGroups.PDF.list().contains(mimeType))
			return new PDFPreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (MimeTypeGroups.OFFICE_DOCUMENTS.list().contains(mimeType))
			return new MsDocumentPreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		if (MimeTypeGroups.VIDEOS.list().contains(mimeType))
			return new VideoPreviewMaker(resourceId, previewsRepoPath,
					entryPoint, realPath, previewUri);
		logger.warn("No preview maker found for mime type :" + mimeType);
		return new UntypedPreviewMaker(resourceId, previewsRepoPath,
				entryPoint, realPath, previewUri);
	}

}
