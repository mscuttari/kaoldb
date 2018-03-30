package it.mscuttari.kaoldbtest;

import android.app.Application;

import it.mscuttari.kaoldb.KaolDB;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KaolDB kaolDB = KaolDB.getInstance();
        kaolDB.setConfig(this, R.xml.persistence);

    }

}
