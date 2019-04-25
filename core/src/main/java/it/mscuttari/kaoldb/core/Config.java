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

import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

class Config {

    /** Maps each database name to its {@link DatabaseObject} */
    private final Map<String, DatabaseObject> mapping;


    /** Whether the debug messages should be enabled or not */
    private boolean debug;


    /**
     * Constructor
     */
    public Config() {
        mapping = new HashMap<>();
        debug = false;
    }


    /**
     * Get an unmodifiable {@link Map} between database name and {@link DatabaseObject}
     *
     * @return database map
     */
    public Map<String, DatabaseObject> getDatabaseMapping() {
        return Collections.unmodifiableMap(mapping);
    }


    /**
     * Get an unmodifiable {@link Collection} of database names
     *
     * @return database names
     */
    public Collection<String> getDatabaseNames() {
        return Collections.unmodifiableCollection(mapping.keySet());
    }


    /**
     * Check if debug mode is enabled
     *
     * @return true if enabled; false otherwise
     */
    public boolean isDebugEnabled() {
        return debug;
    }


    /**
     * Set whether the debug mode should be enabled or not
     *
     * @param enabled       whether to enable or not the debug mode
     */
    public void setDebugMode(boolean enabled) {
        this.debug = enabled;

        if (enabled) {
            LogUtils.i("Debug mode enabled");
        } else {
            LogUtils.i("Debug mode disabled");
        }
    }


    /**
     * Parse the XML configuration file
     *
     * @param xml       the XmlResourceParser instance used to read the configuration file
     *
     * @throws XmlPullParserException in case of parsing error
     * @throws IOException in case of general i/o error
     */
    public void parseConfigFile(XmlResourceParser xml) throws XmlPullParserException, IOException {
        int eventType = xml.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals("databases")) {
                parseDatabasesList(xml);
            }

            eventType = xml.next();
        }
    }


    /**
     * Iterate through databases list
     *
     * @param xml       the XmlResourceParser instance used to read the configuration file
     *
     * @throws XmlPullParserException in case of parsing error
     * @throws IOException in case of general i/o error
     */
    private void parseDatabasesList(XmlResourceParser xml) throws XmlPullParserException, IOException {
        int eventType = xml.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals("database")) {
                parseDatabaseSection(xml);
            }

            eventType = xml.next();
        }
    }


    /**
     * Parse single database section
     *
     * @param xml   the XmlResourceParser instance used to read the configuration file
     *
     * @throws XmlPullParserException in case of parsing error
     * @throws IOException in case of general i/o error
     */
    private void parseDatabaseSection(XmlResourceParser xml) throws XmlPullParserException, IOException {
        LogUtils.v("Parsing database section");
        DatabaseObject database = new DatabaseObject();

        // Name
        database.setName(xml.getAttributeValue(null, "name"));

        // Version
        String version = xml.getAttributeValue(null, "version");

        if (version == null || version.isEmpty())
            throw new InvalidConfigException("[Database \"" + database.getName() + "\"] version not set");

        try {
            database.setVersion(Integer.valueOf(version));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("[Database \"" + database.getName() + "\"] invalid version", e);
        }

        // Schema migrator
        String migrator = xml.getAttributeValue(null, "migrator");

        if (migrator != null && !migrator.isEmpty()) {
            try {
                Class<?> migratorClass = Class.forName(migrator);
                if (DatabaseSchemaMigrator.class.isAssignableFrom(migratorClass)) {
                    database.setSchemaMigrator(migratorClass.asSubclass(DatabaseSchemaMigrator.class));
                } else {
                    throw new InvalidConfigException("[Database \"" + database.getName() + "\"] invalid schema migrator");
                }

            } catch (ClassNotFoundException e) {
                throw new InvalidConfigException("[Database \"" + database.getName() + "\"] invalid schema migrator", e);
            }
        }


        LogUtils.i("Database found: [" +
                "name = " + database.getName() + ", " +
                "version = " + database.getVersion() + ", " +
                "migrator = " + database.getSchemaMigrator().getSimpleName() + "]"
        );


        // Classes
        int eventType = xml.getEventType();

        while (eventType != XmlPullParser.END_TAG || xml.getName().equals("class")) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals("class")) {
                //noinspection UnusedAssignment
                eventType = xml.next();

                try {
                    Class<?> clazz = Class.forName(xml.getText());
                    database.addEntityClass(clazz);

                } catch (ClassNotFoundException e) {
                    throw new InvalidConfigException("[Database \"" + database.getName()+ "\"]: class " + xml.getText() + " not found", e);
                }
            }

            eventType = xml.next();
        }

        // Add database to databases list
        mapping.put(database.getName(), database);
    }

}
