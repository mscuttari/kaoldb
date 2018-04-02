package it.mscuttari.kaoldbtest;

import android.app.Application;
import android.util.Log;
import android.util.TimingLogger;

import com.facebook.stetho.Stetho;

import it.mscuttari.kaoldb.EntityManager;
import it.mscuttari.kaoldb.EntityManagerFactory;
import it.mscuttari.kaoldb.KaolDB;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Stetho
        Stetho.initializeWithDefaults(this);

        // KaolDB
        KaolDB kaolDB = KaolDB.getInstance();
        kaolDB.setConfig(this, R.xml.persistence);
        kaolDB.setDebugMode(true);

        EntityManager em1 = EntityManagerFactory.getInstance().getEntityManager(getApplicationContext(), "test");
        EntityManager em2 = EntityManagerFactory.getInstance().getEntityManager(getApplicationContext(), "test2");
    }

}
