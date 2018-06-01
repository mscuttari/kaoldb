package it.mscuttari.kaoldb.core;

import android.content.Context;
import android.content.res.XmlResourceParser;

import java.util.HashMap;
import java.util.Map;

import it.mscuttari.kaoldb.exceptions.ConfigParseException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

/**
 * Main point of access for the KaolDB framework
 */
public final class KaolDB {

    private static KaolDB instance;
    Config config;
    private Map<String, EntityManager> entityManagers;


    /**
     * Constructor
     */
    private KaolDB() {
        this.config = new Config();
        entityManagers = new HashMap<>();
    }


    /**
     * Get instance
     *
     * @return  singleton instance
     */
    public static KaolDB getInstance() {
        if (instance == null)
            instance = new KaolDB();

        return instance;
    }


    /**
     * Set the framework in debug mode or not (default = false)
     *
     * @param   enabled     whether to enable or not debug logs
     */
    public void setDebugMode(boolean enabled) {
        config.setDebugMode(enabled);
    }


    /**
     * Set configuration
     *
     * @param   resId           resource ID of the XML configuration file
     * @throws  KaolDBException in case of problems (configuration file not readable, invalid format, invalid mapping, etc.)
     */
    public void setConfig(Context context, int resId) {
        // Load configuration file
        LogUtils.d("Loading the configuration file. Resource ID: " + resId);
        XmlResourceParser xml = null;

        try {
            xml = context.getResources().getXml(resId);
        } catch (Exception e) {
            LogUtils.e("Can't load the configuration file. Error message: " + e.getMessage());
            e.printStackTrace();
        }

        // Parse configuration file
        LogUtils.d("Parsing the configuration file");

        try {
            config.parseConfigFile(xml);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ConfigParseException(e.getMessage());
        } finally {
            if (xml != null)
                xml.close();
        }

        LogUtils.i("Configuration loaded");

        // Map the entities
        LogUtils.d("Mapping the entities");

        for (String dbName : config.getDatabaseMapping().keySet()) {
            DatabaseObject database = config.getDatabaseMapping().get(dbName);
            database.setEntitiesMap(EntityUtils.createEntities(database.getEntityClasses()));
        }

        LogUtils.i("Entities mapped");
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
        Map<String, DatabaseObject> mapping = KaolDB.getInstance().config.getDatabaseMapping();
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
