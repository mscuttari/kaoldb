package it.mscuttari.kaoldbtest;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

import java.util.List;

import it.mscuttari.kaoldb.core.EntityManagerFactory;
import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.models.Book;
import it.mscuttari.kaoldbtest.models.Book_;
import it.mscuttari.kaoldbtest.models.Person;
import it.mscuttari.kaoldbtest.models.ThrillerBook;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Stetho
        Stetho.initializeWithDefaults(this);

        // Measure performance
        int cycles = 1;
        float avg = 0;

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

        // Test
        EntityManager em = EntityManagerFactory.getInstance().getEntityManager(getApplicationContext(), "test");
        em.deleteDatabase();

        ThrillerBook book = new ThrillerBook();
        book.title = "In viaggio con PI";
        book.genre = "thriller";
        book.year = 2016;
        em.persist(book);
        book.title = "Mosca";
        em.persist(book);
        book.year = 2017;
        book.prova = "cipollina";
        book.title = "CDP";
        em.persist(book);

        Log.e("KaolDB", "Count: " + em.getRowCount("books"));

        List<Book> books = em.getAll(Book.class);
        for (Book row : books) {
            Log.e("KaolDB", "Book: " + row);
        }

        QueryBuilder<Book> qb = em.getQueryBuilder(Book.class);

        Root<Book> booksRoot = qb.getRoot(Book.class, "b");
        //Root<Person> join = booksRoot.innerJoin(Person.class, "p");

        Expression expression = booksRoot.eq(Book_.title, "In viaggio con PI").or(booksRoot.eq(Book_.year, 2017));

        qb.from(booksRoot);
        qb.where(expression);

        Query<Book> query = qb.build("b");
        Log.e("KaolDB", "Query: " + query.toString());

        List<Book> searchResult = query.getResultList();
        for (Book row : searchResult) {
            Log.e("KaolDB", "Result Book: " + row);
        }


    }

}
