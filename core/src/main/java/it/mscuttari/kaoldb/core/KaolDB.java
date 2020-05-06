/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import android.content.Context;
import android.content.res.XmlResourceParser;

import java.util.Map;

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
    final Config config = new Config();

    /**
     * Private constructor for singleton.
     */
    private KaolDB() {

    }

    /**
     * Get instance.
     *
     * @return singleton instance
     */
    public static KaolDB getInstance() {
        if (instance == null)
            instance = new KaolDB();

        return instance;
    }

    /**
     * Set the framework in debug mode or not (default = <code>false</code>).
     *
     * @param enabled   whether to enable or not debug logs
     */
    public void setDebugMode(boolean enabled) {
        config.setDebugMode(enabled);
    }

    /**
     * Set configuration.
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
            config.parseConfigFile(xml);

        } catch (Exception e) {
            throw new ConfigParseException(e);

        } finally {
            xml.close();
        }

        LogUtils.i("Configuration loaded");

        // Map the entities
        for (String dbName : config.getDatabaseMapping().keySet()) {
            DatabaseObject database = config.getDatabaseMapping().get(dbName);
            database.mapEntities();
        }
    }

    /**
     * Get entity manager for a specific database.
     *
     * @param context           application context
     * @param databaseName      database name
     *
     * @return entity manager
     *
     * @throws IllegalArgumentException if the database doesn't exist
     */
    public EntityManager getEntityManager(@NonNull Context context, @NonNull String databaseName) {
        checkNotNull(databaseName);
        context = context.getApplicationContext();

        if (databaseName.isEmpty()) {
            throw new KaolDBException("Empty database name");
        }

        Map<String, DatabaseObject> mapping = config.getDatabaseMapping();
        DatabaseObject db = mapping.get(databaseName);

        if (db == null)
            throw new IllegalArgumentException("Database \"" + databaseName + "\" not found");

        return EntityManagerImpl.getEntityManager(context, db);
    }

}
