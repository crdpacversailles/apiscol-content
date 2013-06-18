package fr.ac_versailles.crdp.apiscol.content.databaseAccess;

import java.util.ArrayList;
import java.util.Map;

import com.mongodb.MongoException;

import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;

public interface IResourceDataHandler {

	void createResourceEntry(String resourceId) throws DBAccessException;

	void createResourceEntry(String resourceId, ContentType type)
			throws DBAccessException;

	void createResourceEntry(String resourceId, String mainFile)
			throws DBAccessException;

	void createResourceEntry(String resourceId, ContentType type,
			String mainFile) throws DBAccessException;

	void reportFileAddition(String resourceId, String fileName)
			throws InexistentResourceInDatabaseException, DBAccessException;

	String getMainFileForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	void setMainFileForResource(String resourceId, String fileName)
			throws DBAccessException, InexistentResourceInDatabaseException;

	void setMetadataForResource(String resourceId, String metadataId)
			throws DBAccessException, InexistentResourceInDatabaseException;

	String getMetadataForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	void setScormTypeForResource(String string, ContentType type)
			throws DBAccessException, InexistentResourceInDatabaseException;

	String getScormTypeForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	void setUrlForResource(String string, String url) throws DBAccessException,
			InexistentResourceInDatabaseException;

	String getUrlForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	boolean checkMainFileOnResource(String resourceId, ArrayList<String> fileList)
			throws DBAccessException, InexistentResourceInDatabaseException;

	void deleteResourceEntry(String resourceId) throws MongoException,
			InexistentResourceInDatabaseException, DBAccessException;

	void eraseUrlForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	void eraseMainFileForResource(String resourceId) throws DBAccessException,
			InexistentResourceInDatabaseException;

	void updateVersionNumber(String resourceId)
			throws InexistentResourceInDatabaseException, DBAccessException;

	String getEtagForResource(String resourceId)
			throws InexistentResourceInDatabaseException, DBAccessException;

	void deInitialize();

	Map<String, String> getUrlResourcesList() throws DBAccessException;

	void markUrlAsDead(String key, boolean bool) throws InexistentResourceInDatabaseException, DBAccessException;

	void deleteAllDocuments() throws DBAccessException;
	
	void deserializeAndSaveToDataBase(String resourceId, String serializedData) throws DBAccessException;

	String getResourceIdByMetadataId(String metadataId) throws DBAccessException, InexistentResourceInDatabaseException;

}
