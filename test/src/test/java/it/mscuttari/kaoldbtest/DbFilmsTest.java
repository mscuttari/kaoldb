package it.mscuttari.kaoldbtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Calendar;

import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.models.Country;
import it.mscuttari.kaoldbtest.models.Film;
import it.mscuttari.kaoldbtest.models.Person;
import it.mscuttari.kaoldbtest.models.Person_;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DbFilmsTest {

    private KaolDB kdb;
    private static final String databaseName = "films";
    private EntityManager em;


    @Before
    public void setUp() {
        // KaolDB instance
        kdb = KaolDB.getInstance();
        kdb.setConfig(RuntimeEnvironment.application, R.xml.persistence);
        kdb.setDebugMode(true);

        // Entity manager
        em = kdb.getEntityManager(RuntimeEnvironment.application, databaseName);
        em.deleteDatabase();
    }


    @After
    public void tearDown() {
        EntityManager em = kdb.getEntityManager(RuntimeEnvironment.application, databaseName);
        assertTrue(em.deleteDatabase());
    }


    @Test
    public void persistPerson() {
        Person person = new Person(
                "First name",
                "Last name",
                Calendar.getInstance(),
                new Country("USA")
        );

        person.birthDate.set(Calendar.YEAR, 1965);
        person.birthDate.set(Calendar.MONTH, Calendar.APRIL);
        person.birthDate.set(Calendar.DAY_OF_MONTH, 4);

        em.persist(person.country);
        em.persist(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class, "p");

        Expression where = personRoot.eq(Person_.firstName, "First name")
                .and(personRoot.eq(Person_.lastName, "Last name"))
                .and(personRoot.eq(Person_.country, new Country("USA")));

        qb.from(personRoot).where(where);

        Person result = qb.build("p").getSingleResult();
        assertEquals(person, result);

        for (Film film : result.directing) {
            System.out.println("Test");
        }
    }

}
