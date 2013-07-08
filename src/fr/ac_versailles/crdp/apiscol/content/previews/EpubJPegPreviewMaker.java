package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class EpubJPegPreviewMaker extends AbstractPreviewMaker {

	private static final int DEFAULT_PAGES_NUMBER = 10;

	public EpubJPegPreviewMaker(String resourceId,
			String previewsRepoPath, String entryPoint, String realPath,
			String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);

	}

	@Override
	protected void createNewPreview() {
		trackingObject.updateStateAndMessage(States.pending,
				"The document is being converted to jpeg images.");
		StringBuilder pages = new StringBuilder();
		String sourceImageFilePath = ResourceDirectoryInterface.getFilePath(
				resourceId, entryPoint);
		Set<String> outputs = new HashSet<String>();
		outputs.add("image/png");
		List<String> images = ConversionServerInterface.askForConversion(
				sourceImageFilePath, outputs);
		if (images == null) {
			// TODO enregistrer en base l'absence de preview
			String message = "No preview image obtained from conversion server interface for resource"
					+ resourceId;
			logger.error(message);
			trackingObject.updateStateAndMessage(States.aborted, message);
			return;
		}
		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The document has been converted to jpeg images and will be fetched back to ApiScol Content handler.");
		for (int i = 0; i < Math.min(images.size(), DEFAULT_PAGES_NUMBER); i++) {
			writePreviewFileToDisk(images.get(i));
			pages.append("<img src=\"" + previewUri + "/page" + (i + 1)
					+ ".png\" />");

		}
		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The jpeg preview has been returned to ApiScol content. Preview web page is going to be built.");
		FileInputStream is = null;
		String path = realPath + "/templates/pdfpreviewwidget.html";
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"The conversion process failed because of a template reading problem. "
							+ e.getMessage());
			e.printStackTrace();
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("pages", pages.toString());
		tokens.put("preview-id", resourceId);
		MapTokenResolver resolver = new MapTokenResolver(tokens);

		Reader source = new InputStreamReader(is);

		Reader reader = new TokenReplacingReader(source, resolver);

		String htmlWidgetFilePath = previewDirectoryPath + "/widget.html";
		FileUtils.writeDataToFile(reader, htmlWidgetFilePath);
		JSonUtils.convertHtmlFileToJson(htmlWidgetFilePath, "index.html.js");
		String pageHtml = "";
		try {
			pageHtml = FileUtils.readFileAsString(realPath
					+ "/templates/previewpage.html");
			String widgetHtml = FileUtils.readFileAsString(htmlWidgetFilePath);
			pageHtml = pageHtml.replace("WIDGET", widgetHtml);
		} catch (IOException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"The conversion process failed because of a template handling problem. "
							+ e.getMessage());
			e.printStackTrace();
			return;
		}
		String htmlPageFilePath = previewDirectoryPath + "/index.html";
		FileUtils.writeDataToFile(new StringReader(pageHtml), htmlPageFilePath);
		trackingObject.updateStateAndMessage(States.done,
				"The document has been successfully converted to jpeg images.");
	}

}
