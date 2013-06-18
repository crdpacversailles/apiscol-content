package fr.ac_versailles.crdp.apiscol.content;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.Application;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessFactory.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;

public class ApiscolContent extends ServletContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ApiscolContent() {

	}

	public ApiscolContent(Class<? extends Application> appClass) {
		super(appClass);
	}

	public ApiscolContent(Application app) {
		super(app);
	}

	@PreDestroy
	public void deinitialize() {
		IResourceDataHandler dataHandler = null;
		try {
			dataHandler = DBAccessFactory
					.getResourceDataHandler(DBTypes.mongoDB);
		} catch (DBAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dataHandler.deInitialize();
		ResourceApi.stopExecutors();
	}

	@PostConstruct
	public void initialize() {
		// nothing at this time
	}
}
