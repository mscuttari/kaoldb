package it.mscuttari.kaoldb;

import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class Config {

    String databaseName;
    List<Class<?>> classes;
    List<EntityObject> entities;


    /**
     * Parse the XML configuration file
     *
     * @param   xml     XmlResourceParser   the XmlPullParser instance used to read the configuration file
     *
     * @throws  XmlPullParserException      in case of parsing error
     * @throws  IOException                 in case of general i/o error
     */
    void parseConfigFile(XmlResourceParser xml) throws XmlPullParserException, IOException {
        int eventType = xml.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals("database")) {
                parseDatabaseTag(xml);
            }

            eventType = xml.next();
        }

        // Check if the configuration is consistent
        // TODO: checkConfig();
    }


    /**
     * Parse database section
     *
     * @param   xml     XmlResourceParser
     *
     * @throws  XmlPullParserException      in case of parsing error
     * @throws  IOException                 in case of general i/o error
     */
    private void parseDatabaseTag(XmlResourceParser xml) throws XmlPullParserException, IOException {
        databaseName = xml.getAttributeValue(null, "tableName");
        classes = new ArrayList<>();

        int eventType = xml.getEventType();

        while (eventType != XmlPullParser.END_TAG || xml.getName().equals("class")) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals("class")) {
                eventType = xml.next();

                try {
                    classes.add(Class.forName(xml.getText()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            eventType = xml.next();
        }
    }


    /**
     * Check if the configuration is valid
     *
     * @throws  InvalidConfigException if the database name has not been specified
     */
    void check() {
        // Database tableName not empty
        if (databaseName.isEmpty()) {
            throw new InvalidConfigException("Database name not specified");
        }

        // Models tree structure has already been checked while creating the entities
    }

}
