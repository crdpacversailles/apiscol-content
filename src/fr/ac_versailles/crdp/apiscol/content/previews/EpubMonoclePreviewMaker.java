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
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class EpubMonoclePreviewMaker extends AbstractPreviewMaker {

	private static final String TEACHER_ACTION_LABEL = "Action du professeur";
	private static final String LEARNER_ACTION_LABEL = "Action de l'élève";
	private static NamespaceContext ctx = new NamespaceContext() {
		public String getNamespaceURI(String prefix) {
			String uri;
			if (prefix.equals(UsedNamespaces.SVG.getShortHand()))
				uri = UsedNamespaces.SVG.getUri();
			else
				uri = UsedNamespaces.SVG.getUri();
			return uri;
		}

		public Iterator<String> getPrefixes(String val) {
			return null;
		}

		public String getPrefix(String uri) {
			return null;
		}
	};
	private static XPathFactory xPathFactory = XPathFactory.newInstance();
	private static XPath xpath = xPathFactory.newXPath();

	public EpubMonoclePreviewMaker(String resourceId, String previewsRepoPath,
			String entryPoint, String realPath, String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);
		assignNamespaceContext();

	}

	@Override
	protected void createNewPreview() {
		trackingObject
				.updateStateAndMessage(States.pending,
						"The ubz file is being converted to web-compliant format.");
		String ubzFilePath = ResourceDirectoryInterface.getFilePath(resourceId,
				entryPoint);
		try {
			FileUtils.unzipFile(new File(ubzFilePath), previewDirectoryPath);
		} catch (IOException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Impossible to open the ubz file : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		trackingObject.updateStateAndMessage(States.pending,
				"The content of the file has been extracted.");
		DecimalFormat myFormatter = new DecimalFormat("000");
		int counter = 1;
		String filePath;

		StringBuilder accordeonBuilder = new StringBuilder();
		StringBuilder viewsBuilder = new StringBuilder();
		while (true) {
			String formattedCounter = myFormatter.format(counter);
			filePath = new StringBuilder().append(previewDirectoryPath)
					.append("/page").append(formattedCounter).append(".svg")
					.toString();

			File svgFile = new File(filePath);
			if (!svgFile.exists()) {
				break;
			}

			XPathExpression exp0 = null;
			XPathExpression exp1 = null;
			XPathExpression exp2 = null;
			try {
				// select all query terms tag from content response
				exp0 = xpath.compile("//svg:teacherGuide/svg:title");
				exp1 = xpath.compile("//svg:teacherGuide/svg:comment");
				exp2 = xpath.compile("//svg:action");
				InputSource inputSource = new InputSource(
						svgFile.getAbsolutePath());
				Element titleNode = (Element) exp0.evaluate(inputSource,
						XPathConstants.NODE);
				String title = "Page n°" + counter + " ";
				if (titleNode != null && titleNode.hasAttribute("value"))
					title += titleNode.getAttribute("value");

				accordeonBuilder.append("<h1><a href=\"#")
						.append(formattedCounter).append("\">").append(title)
						.append("</a></h1>");
				accordeonBuilder.append("<div>");
				Element commentNode = (Element) exp1.evaluate(inputSource,
						XPathConstants.NODE);

				if (commentNode != null && commentNode.hasAttribute("value")) {
					String comment = commentNode.getAttribute("value");
					accordeonBuilder.append("<p class=\"ui-helper-reset\">")
							.append(comment).append("</p>");
				}
				accordeonBuilder.append("</div>");
				NodeList actionNodes = (NodeList) exp2.evaluate(inputSource,
						XPathConstants.NODESET);
				int actionCount = actionNodes.getLength();
				viewsBuilder
						.append("<div class=\"view\"><img src=\"")
						.append(previewUri)
						.append("/page")
						.append(formattedCounter)
						.append(".thumbnail.jpg\" /><div class=\"actions-container\">");

				for (int i = 0; i < actionCount; i++) {

					Element actionNode = (Element) actionNodes.item(i);
					String task = actionNode.getAttribute("task").toString();
					String owner = actionNode.getAttribute("owner").toString();
					String ownerClass = "";
					String ownerLabel = "";
					String taskId = String.format("%s-%s", formattedCounter,
							actionCount);
					if (owner.contains("0")) {
						ownerClass = "teacher";
						ownerLabel = TEACHER_ACTION_LABEL;
					} else {
						ownerClass = "learner";
						ownerLabel = LEARNER_ACTION_LABEL;
					}
					viewsBuilder.append("<p class=\"").append(ownerClass)
							.append("\" id=\"").append(taskId).append("\">")
							.append(ownerLabel).append(" : ").append(task)
							.append("</p>");
				}
				viewsBuilder.append("</div></div>");
			} catch (XPathExpressionException e) {
				trackingObject.updateStateAndMessage(States.aborted,
						"Problem with xpath expression : " + e.getMessage());
				e.printStackTrace();
				return;

			}
			counter++;
		}
		FileInputStream is = null;
		try {
			is = new FileInputStream(realPath
					+ "/templates/uniboardpreviewwidget.html");
		} catch (FileNotFoundException e) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Problem during templates reading : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("index", accordeonBuilder.toString());
		tokens.put("views", viewsBuilder.toString());
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
				"The ubz preview web page has been succesfully built.");
	}

	private static void assignNamespaceContext() {
		xpath.setNamespaceContext(ctx);
	}

}
