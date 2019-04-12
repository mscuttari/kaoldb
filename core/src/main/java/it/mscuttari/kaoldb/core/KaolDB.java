package it.mscuttari.kaoldb.core;

import android.content.Context;
import android.content.res.XmlResourceParser;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;
import it.mscuttari.kaoldb.exceptions.ConfigParseException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main point of access for the KaolDB framework
 */
public final class KaolDB {

    /** Singleton instance */
    private static KaolDB instance;

    /** Configuration */
    private final Config config = new Config();

    /** Concurrency */
    private final ExecutorService executorService;


    /**
     * Constructor
     */
    private KaolDB() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        this.executorService = new ThreadPoolExecutor(
                cpuCores,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
        );
    }


    /**
     * Get instance
     *
     * @return singleton instance
     */
    public static KaolDB getInstance() {
        if (instance == null)
            instance = new KaolDB();

        return instance;
    }


    /**
     * Set the framework in debug mode or not (default = false)
     *
     * @param enabled   whether to enable or not debug logs
     */
    public void setDebugMode(boolean enabled) {
        getConfig().setDebugMode(enabled);
    }


    /**
     * Get configuration
     *
     * @return configuration object
     */
    Config getConfig() {
        return config;
    }


    /**
     * Set configuration
     *
     * @param resId     resource ID of the XML configuration file
     * @throws KaolDBException in case of problems (configuration file not readable, invalid format, invalid mapping, etc.)
     */
    public void setConfig(@NonNull Context context, @XmlRes int resId) {
        checkNotNull(context);

        // Load configuration file
        LogUtils.d("Loading the configuration file. Resource ID: " + resId);
        XmlResourceParser xml;

        try {
            xml = context.getResources().getXml(resId);
        } catch (Exception e) {
            throw new KaolDBException("Can't load the configuration file", e);
        }

        // Parse configuration file
        LogUtils.d("Parsing the configuration file");

        try {
            getConfig().parseConfigFile(xml);

        } catch (Exception e) {
            throw new ConfigParseException(e);

        } finally {
            xml.close();
        }

        LogUtils.i("Configuration loaded");

        // Map the entities
        for (String dbName : getConfig().getDatabaseMapping().keySet()) {
            DatabaseObject database = getConfig().getDatabaseMapping().get(dbName);
            database.mapEntities();
        }
    }


    /**
     * Get executor service to be used for concurrency
     *
     * @return executor service
     */
    ExecutorService getExecutorService() {
        return executorService;
    }



    /**
     * Get entity manager for a specific database
     *
     * @param context           context
     * @param databaseName      database name
     *
     * @return entity manager
     */
    public EntityManager getEntityManager(@NonNull Context context, @NonNull String databaseName) {
        context = context.getApplicationContext();

        if (databaseName == null || databaseName.isEmpty()) {
            throw new KaolDBException("Empty database name");
        }

        Map<String, DatabaseObject> mapping = KaolDB.getInstance().getConfig().getDatabaseMapping();
        DatabaseObject database = mapping.get(databaseName);
        return EntityManagerImpl.getEntityManager(context, database);
    }

}
