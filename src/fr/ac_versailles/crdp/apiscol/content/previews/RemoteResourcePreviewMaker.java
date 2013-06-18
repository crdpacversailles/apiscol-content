package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class RemoteResourcePreviewMaker extends AbstractPreviewMaker {

	public RemoteResourcePreviewMaker(String resourceId,
			String previewsRepoPath, String entryPoint, String realPath,
			String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);

	}

	@Override
	protected void createNewPreview() {
		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The preview template is going to be copied and parameterized for this web page.");
		FileInputStream is = null;
		try {
			is = new FileInputStream(realPath
					+ "/templates/remoteresourcepreviewwidget.html");
		} catch (FileNotFoundException e) {
			trackingObject.updateStateAndMessage(
					States.aborted,
					"Impossible to copye the preview template : "
							+ e.getMessage());
			e.printStackTrace();
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("src", entryPoint);
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
			trackingObject.updateStateAndMessage(
					States.aborted,
					"Impossible to copye the preview template : "
							+ e.getMessage());
			e.printStackTrace();
			return;
		}
		String htmlPageFilePath = previewDirectoryPath + "/index.html";
		FileUtils.writeDataToFile(new StringReader(pageHtml), htmlPageFilePath);
		trackingObject
				.updateStateAndMessage(
						States.done,
						"The preview template has been successfully copied and parameterized for this web page.");
	}
}
