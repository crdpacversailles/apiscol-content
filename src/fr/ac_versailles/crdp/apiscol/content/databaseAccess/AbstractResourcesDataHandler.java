package fr.ac_versailles.crdp.apiscol.content.databaseAccess;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractResourcesDataHandler implements
		IResourceDataHandler {
	protected static Logger logger;

	public AbstractResourcesDataHandler() throws DBAccessException {
		createLogger();
		dbConnect();
	}

	@Override
	public void deInitialize() {
		dbDisconnect();

	}

	protected abstract void dbDisconnect();

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

	@Override
	public void reportFileAddition(String resourceId, String fileName)
			throws InexistentResourceInDatabaseException, DBAccessException {
		addMainFileToResourceIfNot(resourceId, fileName);
	}

	abstract protected void addMainFileToResourceIfNot(String resourceId,
			String fileName) throws DBAccessException,
			InexistentResourceInDatabaseException;

	abstract protected void addMainFileToResource(String resourceId,
			String fileName) throws DBAccessException,
			InexistentResourceInDatabaseException;

	abstract protected void dbConnect() throws DBAccessException;

	@Override
	public void createResourceEntry(String resourceId) throws DBAccessException {
		createResourceEntry(resourceId, ContentType.asset, null);
		try {
			updateVersionNumber(resourceId);
		} catch (InexistentResourceInDatabaseException e) {
			String message = String
					.format("Impossible to update the version number of the resource %s that where just created",
							resourceId);
			logger.error(message);
			throw new DBAccessException(message);
		}
	}

	@Override
	public void createResourceEntry(String resourceId, ContentType type)
			throws DBAccessException {
		createResourceEntry(resourceId, type, null);
	}

	@Override
	public void createResourceEntry(String resourceId, String mainFile)
			throws DBAccessException {
		createResourceEntry(resourceId, ContentType.asset, mainFile);
	}

	@Override
	abstract public void createResourceEntry(String resourceId,
			ContentType type, String mainFile) throws DBAccessException;

	@Override
	abstract public String getMainFileForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException;

	@Override
	public void setMainFileForResource(String resourceId, String fileName)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addMainFileToResource(resourceId, fileName);
	}

	@Override
	public void eraseMainFileForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addMainFileToResource(resourceId, null);
	}

	@Override
	public void setMetadataForResource(String resourceId, String metadataId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addMetadataToResource(resourceId, metadataId);
	}

	@Override
	public void setScormTypeForResource(String resourceId, ContentType scormType)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addScormTypeToResource(resourceId, scormType);
	}

	@Override
	public abstract String getScormTypeForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException;

	@Override
	public void setUrlForResource(String resourceId, String url)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addUrlToResource(resourceId, url);
	}

	@Override
	public void eraseUrlForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addUrlToResource(resourceId, null);
	}

	protected abstract void addUrlToResource(String resourceId, String url)
			throws DBAccessException, InexistentResourceInDatabaseException;

	@Override
	public abstract String getUrlForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException;

	protected abstract void addScormTypeToResource(String resourceId,
			ContentType scormType) throws DBAccessException,
			InexistentResourceInDatabaseException;

	abstract protected void addMetadataToResource(String resourceId,
			String metadataId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	@Override
	public boolean checkMainFileOnResource(String resourceId,
			ArrayList<String> fileList) throws DBAccessException,
			InexistentResourceInDatabaseException {
		if (fileList.isEmpty()) {
			eraseMainfileForRessource(resourceId);
			logger.info(String
					.format("There are no more files in resource %s, the main file reference has been erased ",
							resourceId));
			return true;
		} else if (!fileList.contains(getMainFileForResource(resourceId))) {
			addMainFileToResource(resourceId, fileList.get(0));
			logger.info(String
					.format("Main file for resource %s has been erased, main file was arbitrarely set to ",
							fileList.get(0)));
			return true;
		} else return false;

	}

	protected abstract void eraseMainfileForRessource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException;

	protected void serializeAndSaveToDisk(String resourceId) {
		ResourceDirectoryInterface
				.saveToDisk(resourceId, serialize(resourceId));

	}

	protected abstract String serialize(String resourceId);

	public abstract void deserializeAndSaveToDataBase(String resourceId, String serializedData) throws DBAccessException;

}
