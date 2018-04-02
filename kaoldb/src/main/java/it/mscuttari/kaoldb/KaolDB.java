package it.mscuttari.kaoldb;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import it.mscuttari.kaoldb.exceptions.ConfigParseException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

public final class KaolDB {

    private static KaolDB instance;
    Config config;


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
     * @param   enabled     boolean     whether to enable or not debug logs
     */
    public void setDebugMode(boolean enabled) {
        config.debug = enabled;
    }


    /**
     * Set configuration
     *
     * @param   resId   int     resource ID of the XML configuration file
     * @throws  KaolDBException in case of problems (configuration file not readable, invalid format, invalid mapping, etc.)
     */
    public void setConfig(Context context, int resId) {
        XmlResourceParser xml = null;

        try {
            xml = context.getResources().getXml(resId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Can't load the configuration file. Error message: " + e.getMessage());
        }

        try {
            config.parseConfigFile(xml);
        } catch (Exception e) {
            throw new ConfigParseException(e.getMessage());
        } finally {
            if (xml != null)
                xml.close();
        }

        for (String dbName : config.mapping.keySet()) {
            DatabaseObject database = config.mapping.get(dbName);
            database.entities = TableUtils.createEntities(database.classes);
        }
    }

}
