package it.mscuttari.kaoldb.core;

import android.content.Context;
import android.content.res.XmlResourceParser;

import java.util.Map;

import it.mscuttari.kaoldb.exceptions.ConfigParseException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

/**
 * Main point of access for the KaolDB framework
 */
public final class KaolDB {

    private static KaolDB instance;
    private final Config config;


    /**
     * Constructor
     */
    private KaolDB() {
        this.config = new Config();
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
        getConfig().setDebugMode(enabled);
    }


    /**
     * Get configuration
     *
     * @return  configuration object
     */
    Config getConfig() {
        return config;
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
            getConfig().parseConfigFile(xml);
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

        for (String dbName : getConfig().getDatabaseMapping().keySet()) {
            DatabaseObject database = getConfig().getDatabaseMapping().get(dbName);
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

        Map<String, DatabaseObject> mapping = KaolDB.getInstance().getConfig().getDatabaseMapping();
        DatabaseObject database = mapping.get(databaseName);
        return new EntityManagerImpl(context, database);
    }

}
