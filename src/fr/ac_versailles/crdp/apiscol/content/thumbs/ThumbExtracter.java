package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.util.Collection;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;

public interface ThumbExtracter {

	Map<String, Point> getThumbsFromResource(String resourceId,
			IResourceDataHandler resourceDataHandler, String baseUrl,
			String mainFileName, double minDimensionsSum) throws ResourceDirectoryNotFoundException,
			DBAccessException, InexistentResourceInDatabaseException;

	Map<String, Point> getThumbsFromPreview(String resourceId,
			String previewsRepoPath, String baseUrl);

}
