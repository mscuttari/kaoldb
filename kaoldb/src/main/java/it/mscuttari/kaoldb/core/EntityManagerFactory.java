package it.mscuttari.kaoldb.core;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

public final class EntityManagerFactory {

    private static EntityManagerFactory instance;
    private Map<String, EntityManager> entityManagers;


    /**
     * Constructor
     */
    private EntityManagerFactory() {
        entityManagers = new HashMap<>();
    }


    /**
     * Get instance
     *
     * @return  singleton instance
     */
    public static EntityManagerFactory getInstance() {
        if (instance == null)
            instance = new EntityManagerFactory();

        return instance;
    }


    /**
     * Get entity manager for a specific database
     *
     * @param   context         context
     * @param   databaseName    database name
     *
     * @return  entity manager
     */
    public EntityManager getEntityManager(Context context, String databaseName) {
        context = context.getApplicationContext();

        if (databaseName == null || databaseName.isEmpty()) {
            throw new KaolDBException("Empty database name");
        }

        // Check if the entity manager for the specifid database already exists
        if (entityManagers.containsKey(databaseName)) {
            EntityManager em = entityManagers.get(databaseName);
            if (em != null) return em;
        }

        // Create a new entity manager
        LogUtils.d("Creating entity manager for database \"" + databaseName + "\"");
        Map<String, DatabaseObject> mapping = KaolDB.getInstance().config.mapping;
        DatabaseObject database = mapping.get(databaseName);

        if (database == null) {
            throw new KaolDBException("Database " + databaseName + " not found");
        }

        EntityManager em = new EntityManagerImpl(context, database);
        entityManagers.put(databaseName, em);
        LogUtils.i("Entity manager for database \"" + databaseName + "\" has been created");

        return em;
    }

}
