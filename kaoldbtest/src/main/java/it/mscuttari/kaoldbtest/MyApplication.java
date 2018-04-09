package it.mscuttari.kaoldbtest;

import android.app.Application;
import android.util.Log;
import android.util.TimingLogger;

import com.facebook.stetho.Stetho;

import java.util.List;

import it.mscuttari.kaoldb.EntityManagerFactory;
import it.mscuttari.kaoldb.KaolDB;
import it.mscuttari.kaoldb.EntityManager;
import it.mscuttari.kaoldb.query.From;
import it.mscuttari.kaoldb.query.Join;
import it.mscuttari.kaoldb.query.QueryBuilder;
import it.mscuttari.kaoldbtest.models.Book;
import it.mscuttari.kaoldbtest.models.FantasyBook;
import it.mscuttari.kaoldbtest.models.Genre;
import it.mscuttari.kaoldbtest.models.Person;
import it.mscuttari.kaoldbtest.models.ThrillerBook;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        int cycles = 1;
        float avg = 0;

        // Stetho
        Stetho.initializeWithDefaults(this);

        for (int i = 0; i< cycles; i++) {
            long time = System.nanoTime();

            // KaolDB
            KaolDB kaolDB = KaolDB.getInstance();
            kaolDB.setConfig(this, R.xml.persistence);
            kaolDB.setDebugMode(true);

            long difference = System.nanoTime() - time;
            avg += difference;
        }

        avg /= cycles;
        avg /= 1000000;

        Log.e("KaolDB", "Time: " + avg + "ms");

        EntityManager em = EntityManagerFactory.getInstance().getEntityManager(getApplicationContext(), "test");
        em.deleteDatabase();

        ThrillerBook book = new ThrillerBook();
        book.title = "In viaggio con PI";
        book.genre = "thriller";
        book.year = 2016;
        em.persist(book);
        book.title = "Mosca";
        em.persist(book);
        book.title = "CDP";
        em.persist(book);

        Log.e("KaolDB", "Count: " + em.getRowCount("books"));

        List<Book> books = em.getAll(Book.class);
        for (Book row : books) {
            Log.e("KaolDB", "Book: " + row);
        }

        QueryBuilder qb = em.getQueryBuilder();
        From from = qb.from(Book.class, "b");
        //Join join = from.join(Person.class, "p");
        Log.e("KaolDB", from.toString());

    }

}
