package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.w3c.dom.Document;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.HTMLUtils;
import fr.ac_versailles.crdp.apiscol.utils.XMLUtils;

public class XHTMLRepresentationBuilder extends
		AbstractRepresentationBuilder<String> {

	private AbstractRepresentationBuilder<Document> innerBuilder;

	public XHTMLRepresentationBuilder() {
		innerBuilder = new XMLRepresentationBuilder();
	}

	@Override
	public String getLinkUpdateProcedureRepresentation(UriInfo uriInfo) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				realPath + "/xsl/resourceXMLToHTMLTransformer.xsl",
				innerBuilder.getResourceRepresentation(realPath, uriInfo,
						apiscolInstanceName, resourceId, editUri)));
	}

	@Override
	public String getCompleteResourceListRepresentation(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, int start, int rows,
			String editUri) throws DBAccessException {
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				realPath + "/xsl/resourcesListXMLToHTMLTransformer.xsl",
				(Document) innerBuilder.getCompleteResourceListRepresentation(
						realPath, uriInfo, apiscolInstanceName, start, rows,
						editUri)));
	}

	@Override
	public String selectResourceFollowingCriterium(String realPath,
			UriInfo uriInfo, String apiscolInstanceName,
			ISearchEngineResultHandler handler, int start, int rows,
			String editUri) throws DBAccessException {
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				realPath + "/xsl/resourcesListXMLToHTMLTransformer.xsl",
				innerBuilder.selectResourceFollowingCriterium(realPath,
						uriInfo, apiscolInstanceName, handler, start, rows,
						editUri)));
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_HTML_TYPE;
	}

	@Override
	public String getResourceStringRepresentation(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId,
			String editUri) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getFileSuccessfulDestructionReport(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId,
			String fileName) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getInexistentFileDestructionAttemptReport(String realPath,
			UriInfo uriInfo, String resourceId, String fileName) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceSuccessfulDestructionReport(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId,
			String warnings) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceUnsuccessfulDestructionReport(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId,
			String warnings) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getSuccessfullOptimizationReport(String realPath,
			UriInfo uriInfo) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getSuccessfulGlobalDeletionReport() {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, UriInfo uriInfo,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceTechnicalInformations(String realPath,
			UriInfo uriInfo, String apiscolInstanceName, String resourceId)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public Object getRefreshProcessRepresentation(
			Integer refreshProcessIdentifier, UriInfo uriInfo,
			RefreshProcessRegistry refreshProcessRegistry) {
		// TODO Auto-generated method stub
		return null;
	}

}
