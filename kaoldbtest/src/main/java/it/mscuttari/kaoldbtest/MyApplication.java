package it.mscuttari.kaoldbtest;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

import java.util.concurrent.Callable;

import it.mscuttari.kaoldb.core.EntityManagerFactory;
import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.models.FantasyFilm;
import it.mscuttari.kaoldbtest.models.FantasyFilm_;
import it.mscuttari.kaoldbtest.models.Film;
import it.mscuttari.kaoldbtest.models.Film_;
import it.mscuttari.kaoldbtest.models.Genre;
import it.mscuttari.kaoldbtest.models.Person;
import it.mscuttari.kaoldbtest.models.Person_;

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
        EntityManagerFactory emf = EntityManagerFactory.getInstance();
        EntityManager em = emf.getEntityManager(getApplicationContext(), "films");
        em.deleteDatabase();

        // Create fantasy genre
        Genre genre = new Genre();
        genre.name = "Fantasy";
        em.persist(genre);

        // Create film
        Person director = new Person();
        director.firstName = "Mario";
        director.lastName = "Rossi";
        em.persist(director);

        FantasyFilm film = new FantasyFilm();
        film.title = "Avengers - Infinity War";
        film.year = 2018;
        film.director = director;
        em.persist(film);

        // Get film
        QueryBuilder<FantasyFilm> qb = em.getQueryBuilder(FantasyFilm.class);
        Root<FantasyFilm> root = qb.getRoot(FantasyFilm.class, "f");
        Root<Person> rootJoin = root.innerJoin(Person.class, "p", FantasyFilm_.director);
        Expression where = root.eq(FantasyFilm_.genre, genre).and(root.eq(FantasyFilm_.director, director));
        Query<FantasyFilm> query =  qb.from(rootJoin).build("f");
        //Query<FantasyFilm> query2 = qb.from(rootJoin).build("f");
        Log.e("KaolDB", "Query: " + query);
        //Log.e("KaolDB", "Query2: " + query2);
        Film queryResult = query.getSingleResult();
    }

}
