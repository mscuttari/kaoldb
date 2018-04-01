package it.mscuttari.kaoldb;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import it.mscuttari.kaoldb.exceptions.ConfigParseException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

public final class KaolDB {

    private static KaolDB instance;
    private Config config;

    private KaolDB() {

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
            config = new Config();
            config.parseConfigFile(xml);
            config.check();
        } catch (Exception e) {
            throw new ConfigParseException(e.getMessage());
        } finally {
            if (xml != null)
                xml.close();
        }

        config.entities = TableManager.createEntities(config.classes);

        // TODO: remove
        for (EntityObject entity : config.entities) {
            Log.e(LOG_TAG, entity.toString());
            Log.e(LOG_TAG, "Create table SQL: " + TableManager.getCreateTableSql(entity));
        }
    }


    public synchronized long create(Object obj) throws Exception {
        throw new Exception("Not implemented");
    }


    public synchronized long update(Object obj) throws Exception {
        throw new Exception("Not implemented");
    }


    public synchronized boolean delete(Object obj) throws Exception {
        throw new Exception("Not implemented");
    }

}
