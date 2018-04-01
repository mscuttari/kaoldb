package it.mscuttari.kaoldbtest;

import android.app.Application;
import android.util.Log;
import android.util.TimingLogger;

import it.mscuttari.kaoldb.KaolDB;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        long start = System.currentTimeMillis();

        KaolDB kaolDB = KaolDB.getInstance();
        kaolDB.setConfig(this, R.xml.persistence);

        long time = System.currentTimeMillis() - start;

        Log.e("KaolDB", "Time elapsed: " + time + "ms");

    }

}
