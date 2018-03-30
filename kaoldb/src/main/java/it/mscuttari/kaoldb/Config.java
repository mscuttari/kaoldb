package it.mscuttari.kaoldb;

import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class Config {

    public String databaseName;
    public List<Class<?>> classes;
    public List<EntityObject> entities;


    /**
     * Parse the XML configuration file
     *
     * @param   xml     XmlResourceParser   the XmlPullParser instance used to read the configuration file
     *
     * @throws  XmlPullParserException      in case of parsing error
     * @throws  IOException                 in case of general i/o error
     */
    public void parseConfigFile(XmlResourceParser xml) throws XmlPullParserException, IOException {
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
     * @return  boolean     true if valid
     */
    boolean checkConfig() {
        // Database tableName not empty
        if (databaseName.isEmpty()) return false;

        // TODO: check models tree structure

        return true;
    }

}
