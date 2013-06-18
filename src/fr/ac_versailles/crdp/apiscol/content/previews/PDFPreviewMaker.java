package fr.ac_versailles.crdp.apiscol.content.previews;

public class PDFPreviewMaker extends OfficeDocumentPreviewMaker {

	private static final int DEFAULT_PAGES_NUMBER = 10;

	public PDFPreviewMaker(String resourceId, String previewsRepoPath,
			String entryPoint, String realPath, String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);
	}
}
