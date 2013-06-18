package fr.ac_versailles.crdp.apiscol.content.databaseAccess;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.database.MongoUtils;

public class MongoResourceDataHandler extends AbstractResourcesDataHandler {

	public enum DBKeys {
		id("_id"), mainFile("main"), type("type"), metadata("metadata"), url(
				"url"), etag("etag"), deadLink("dead_link");
		private String value;

		private DBKeys(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private GregorianCalendar calendar;

	public MongoResourceDataHandler() throws DBAccessException {
		super();
		calendar = new GregorianCalendar();
	}

	private static final String DB_NAME = "apiscol";
	private static final String COLLECTION_NAME = "resources";
	private static DBCollection resourcesCollection;
	private static Mongo mongo;

	@Override
	public void createResourceEntry(String resourceId, ContentType type,
			String mainFile) throws DBAccessException {
		BasicDBObject resource = new BasicDBObject();
		resource.put(DBKeys.id.toString(), resourceId);
		resource.put(DBKeys.type.toString(), type.toString());
		if (mainFile != null && !mainFile.isEmpty())
			resource.put(DBKeys.mainFile.toString(), mainFile);
		resource.put(DBKeys.etag.toString(), 0);
		try {
			resourcesCollection.insert(resource);

		} catch (MongoException e) {
			logger.error("Error while trying to insert  new resource into resource collection "
					+ e.getMessage());
			throw new DBAccessException(
					"Error while trying to insert  new resource into resource collection "
							+ e.getMessage());
		}
	}

	@Override
	protected void dbConnect() throws DBAccessException {
		if (mongo != null) {
			return;
		}
		mongo = MongoUtils.getMongoConnection();
		resourcesCollection = MongoUtils.getCollection(DB_NAME,
				COLLECTION_NAME, mongo);

	}

	@Override
	protected void dbDisconnect() {
		MongoUtils.dbDisconnect(mongo);
	}

	private DBObject getResourceById(String resourceId)
			throws InexistentResourceInDatabaseException, DBAccessException {
		BasicDBObject query = new BasicDBObject();
		query.put(DBKeys.id.toString(), resourceId);
		DBObject resource = null;
		try {
			resource = resourcesCollection.findOne(query);
		} catch (MongoException e) {
			String message = "Error while trying to read in resource collection "
					+ e.getMessage();
			logger.error(message);
			throw new DBAccessException(message);
		}

		if (resource == null) {
			logger.warn(String
					.format("Tried to fetch resource %s from database, but it doesn't exist",
							resourceId));
			throw new InexistentResourceInDatabaseException(
					String.format(
							"Asked content database for resource %s. But there is no entry for this resource. Your data must be in inconsistent state.",
							resourceId));
		}
		return resource;

	}

	@Override
	protected void addMainFileToResourceIfNot(String resourceId, String fileName)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId, ContentType.asset);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
				throw e2;
			}
		}
		String mainFile = (String) resource.get(DBKeys.mainFile.toString());
		if (mainFile == null || mainFile.isEmpty()) {
			resource.put(DBKeys.mainFile.toString(), fileName);

			try {
				saveAndUpdateLastModified(resource);

			} catch (MongoException e) {
				String message = "Error while trying to save resource to collection "
						+ e.getMessage();
				logger.error(message);
				throw new DBAccessException(message);
			}

		}

	}

	private void saveAndUpdateLastModified(DBObject resource) {
		resourcesCollection.save(resource);
		// TODO last modified n'est pas mis à jour ?
	}

	@Override
	public String getMainFileForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e) {
			String message = "Error while checking mainFile, see log for precisions /"
					+ e.getMessage();
			logger.error(message);
			throw e;
		}
		return (String) resource.get(DBKeys.mainFile.toString());
	}

	@Override
	protected void addMainFileToResource(String resourceId, String fileName)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " You tried to assign mainfile to a resource that is not registred in the database. An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
			}
		}
		if (StringUtils.isBlank(fileName))
			resource.removeField(DBKeys.mainFile.toString());
		else
			resource.put(DBKeys.mainFile.toString(), fileName);

		try {
			saveAndUpdateLastModified(resource);
		} catch (MongoException e) {
			throw new DBAccessException(
					"Error while trying to save resource to collection "
							+ e.getMessage());
		}
	}

	@Override
	protected void eraseMainfileForRessource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		addMainFileToResource(resourceId, null);
	}

	@Override
	protected void addMetadataToResource(String resourceId, String metadataId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " You tried to assign metadata to a resource that is not registred in the database.An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
			}
		}
		resource.put(DBKeys.metadata.toString(), metadataId);
		try {
			saveAndUpdateLastModified(resource);
		} catch (MongoException e) {
			throw new DBAccessException(
					"Error while trying to save resource to collection "
							+ e.getMessage());
		}

	}

	public String getMetadataForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e) {
			logger.error("Error while checking metadata, see log for precisions /"
					+ e.getMessage());
			throw e;
		}
		return (String) resource.get(DBKeys.metadata.toString());
	}

	@Override
	protected void addScormTypeToResource(String resourceId,
			ContentType scormType) throws DBAccessException,
			InexistentResourceInDatabaseException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " You tried to assign scormtype to a resource that is not registred in the database. An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
				return;
			}
		}
		resource.put(DBKeys.type.toString(), scormType.toString());
		try {
			saveAndUpdateLastModified(resource);
		} catch (MongoException e) {
			throw new DBAccessException(
					"Error while trying to save resource to collection "
							+ e.getMessage());
		}
	}

	@Override
	public String getScormTypeForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		resource = getResourceById(resourceId);
		String type = (String) resource.get(DBKeys.type.toString());
		if (StringUtils.isBlank(type))
			logger.error(String.format(
					"The type should not be blank for resource %s", resourceId));
		return type;
	}

	@Override
	protected void addUrlToResource(String resourceId, String url)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		// TODO discutable d'essayer de toujours
		// récupérer.InexistentResourceInDatabaseException
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " You tried to assign url to a resource that is not registred in the database. An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
				return;
			}
		}
		if (StringUtils.isBlank(url))
			resource.removeField(DBKeys.url.toString());
		else
			resource.put(DBKeys.url.toString(), url);
		try {
			saveAndUpdateLastModified(resource);
		} catch (MongoException e) {
			throw new DBAccessException(
					"Error while trying to save resource to collection "
							+ e.getMessage());
		}
	}

	@Override
	public String getUrlForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		DBObject resource = null;
		resource = getResourceById(resourceId);
		return (String) resource.get(DBKeys.url.toString());
	}

	@Override
	public void deleteResourceEntry(String resourceId) throws MongoException,
			InexistentResourceInDatabaseException, DBAccessException {
		resourcesCollection.remove(getResourceById(resourceId));

	}

	@Override
	public void updateVersionNumber(String resourceId)
			throws InexistentResourceInDatabaseException, DBAccessException {
		DBObject resource = getResourceById(resourceId);
		long version = calendar.getTimeInMillis();
		resource.put(DBKeys.etag.toString(), version);
		resourcesCollection.save(resource);
		serializeAndSaveToDisk(resourceId);
	}

	@Override
	public String getEtagForResource(String resourceId)
			throws InexistentResourceInDatabaseException, DBAccessException {
		return String.format("data-version-%s", getResourceById(resourceId)
				.get(DBKeys.etag.toString()));
	}

	@Override
	public Map<String, String> getUrlResourcesList() throws DBAccessException {
		BasicDBObject query = new BasicDBObject();
		query.put(DBKeys.type.toString(), ContentType.url.toString());
		DBCursor cursor = resourcesCollection.find(query);
		DBObject resource;
		Map<String, String> map = new HashMap<String, String>();
		while (cursor.hasNext()) {
			resource = cursor.next();
			map.put(resource.get(DBKeys.id.toString()).toString(), resource
					.get(DBKeys.url.toString()).toString());
		}
		return map;
	}

	@Override
	public void markUrlAsDead(String resourceId, boolean bool)
			throws InexistentResourceInDatabaseException, DBAccessException {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e1) {
			logger.error(e1.getMessage()
					+ " An entry for the resource will be created for safety reason.");
			createResourceEntry(resourceId, ContentType.asset);
			try {
				resource = getResourceById(resourceId);
			} catch (InexistentResourceInDatabaseException e2) {
				logger.error(e2.getMessage()
						+ " Failes to create the entry for this resource, unknown reason.");
				throw e2;
			}
		}
		resource.put(DBKeys.deadLink.toString(), bool);
		try {
			saveAndUpdateLastModified(resource);
		} catch (MongoException e) {
			String message = "Error while trying to save resource to collection "
					+ e.getMessage();
			logger.error(message);
			throw new DBAccessException(message);
		}
	}

	@Override
	public void deleteAllDocuments() throws DBAccessException {
		resourcesCollection.drop();

	}

	@Override
	protected String serialize(String resourceId) {
		DBObject resource = null;
		try {
			resource = getResourceById(resourceId);
		} catch (InexistentResourceInDatabaseException e) {
			logger.error(e.getMessage() + "Impossible to find the resource "
					+ resourceId + " when asked to serialize it.");
			e.printStackTrace();

		} catch (DBAccessException e) {
			logger.error(e.getMessage()
					+ "Impossible to connect to database when asked to serialize resource "
					+ resourceId + ".");
			e.printStackTrace();
		}
		return resource.toString();
	}

	@Override
	public void deserializeAndSaveToDataBase(String resourceId,
			String serializedData) throws DBAccessException {
		Object o = JSON.parse(serializedData);
		DBObject resource = (DBObject) o;
		// TODO vérifier cohérence dbobject resourceid
		try {
			resourcesCollection.insert(resource);
		} catch (MongoException e) {
			String error = "Error while trying to insert  deserialized resource "
					+ resourceId
					+ " into resource collection "
					+ e.getMessage();
			logger.error(error);
			throw new DBAccessException(error);
		}

	}

	@Override
	public String getResourceIdByMetadataId(String metadataId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		BasicDBObject query = new BasicDBObject();
		query.put(DBKeys.metadata.toString(), metadataId);
		DBObject resource = null;
		try {
			resource = resourcesCollection.findOne(query);
		} catch (MongoException e) {
			String message = "Error while trying to read in resource collection "
					+ e.getMessage();
			logger.error(message);
			throw new DBAccessException(message);
		}

		if (resource == null) {
			logger.warn(String
					.format("Tried to fetch resource with metadata %s from database, but it doesn't exist",
							metadataId));
			// TODO correspond pas exactement
			throw new InexistentResourceInDatabaseException(
					String.format(
							"Asked content ws database for resource width metadata %s. But there is no entry for this resource."
									+ "Your data must be in inconsistent state or you did not provide a resource for this metadata document.",
							metadataId));
		}
		return resource.get(DBKeys.id.toString()).toString();
	}
}
