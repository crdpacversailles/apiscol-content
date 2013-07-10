package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class EpubMonoclePreviewMaker extends AbstractPreviewMaker {

	public EpubMonoclePreviewMaker(String resourceId, String previewsRepoPath,
			String entryPoint, String realPath, String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);

	}

	@Override
	protected void createNewPreview() {
		trackingObject.updateStateAndMessage(States.pending,
				"The ubz file is being converted to web-compliant format.");
		String epubFilePath = ResourceDirectoryInterface.getFilePath(
				resourceId, entryPoint);
		try {
			FileUtils.unzipFile(new File(epubFilePath), previewDirectoryPath);
		} catch (IOException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Impossible to open the ubz file : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		trackingObject.updateStateAndMessage(States.pending,
				"The content of the file has been extracted.");
		EpubReader epubReader = new EpubReader();
		Book book = null;
		StringBuilder bookComponentsBuilder = new StringBuilder();
		StringBuilder bookHTMLBuilder = new StringBuilder();
		StringBuilder bookContentBuilder = new StringBuilder();
		String bookTitle = "Sans titre";
		String bookCreator = "Auteur inconnu";

		try {
			book = epubReader.readEpub(new FileInputStream(epubFilePath));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// bookTitle = book.getMetadata().getTitles().get(0);
		// bookCreator = book.getMetadata().getAuthors().get(0).toString();
		Spine spine = book.getSpine();
		List<SpineReference> references = spine.getSpineReferences();
		Iterator<SpineReference> it = references.iterator();
		while (it.hasNext()) {
			SpineReference spineReference = (SpineReference) it.next();
			Resource resource = spineReference.getResource();
			Boolean hasNext = it.hasNext();
			bookComponentsBuilder.append("'").append(resource.getHref())
					.append("'").append(hasNext ? "," : "");
			bookContentBuilder.append("{title: \"").append(resource.getTitle())
					.append("\",src: \"").append(resource.getHref())
					.append("\"}").append(hasNext ? "," : "");
			try {
				bookHTMLBuilder
						.append("'")
						.append(resource.getHref())
						.append("' : '")
						.append(new String(resource.getData(), "UTF-8")
								.replace("'", "&quote;").replaceAll("\n", ""))
						.append("'").append(hasNext ? "," : "");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		FileInputStream is = null;
		try {
			is = new FileInputStream(realPath
					+ "/templates/epubpreviewwidget.html");
		} catch (FileNotFoundException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Problem during templates reading : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("book-components", bookComponentsBuilder.toString());
		tokens.put("book-content", bookContentBuilder.toString());
		tokens.put("book-html", bookHTMLBuilder.toString());
		tokens.put("book-title", bookTitle);
		tokens.put("book-creator", bookCreator);
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
					"Problem during templates handling : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		String htmlPageFilePath = previewDirectoryPath + "/index.html";
		FileUtils.writeDataToFile(new StringReader(pageHtml), htmlPageFilePath);
		trackingObject.updateStateAndMessage(States.done,
				"The epub preview web page has been succesfully built.");
	}
}
