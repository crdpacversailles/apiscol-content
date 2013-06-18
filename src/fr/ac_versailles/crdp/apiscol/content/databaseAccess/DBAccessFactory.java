package fr.ac_versailles.crdp.apiscol.content.databaseAccess;

import fr.ac_versailles.crdp.apiscol.database.DBAccessException;

public class DBAccessFactory {
	public enum DBTypes {
		mongoDB("mongodb");
		private String value;

		private DBTypes(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public static IResourceDataHandler getResourceDataHandler(DBTypes dbType)
			throws DBAccessException {
		switch (dbType) {
		case mongoDB:
			return new MongoResourceDataHandler();
		default:
			// TODO gérer le cas d'une demande fantaisiste
			break;
		}
		return null;
	}

}
