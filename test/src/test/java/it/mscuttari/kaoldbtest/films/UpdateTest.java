package it.mscuttari.kaoldbtest.films;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class UpdateTest {

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
    public void updatePerson() {
        Person person = new Person(
                "Robert",
                "Downey Jr.",
                Calendar.getInstance(),
                new Country("USA")
        );

        person.birthDate.set(Calendar.YEAR, 1965);
        person.birthDate.set(Calendar.MONTH, Calendar.APRIL);
        person.birthDate.set(Calendar.DAY_OF_MONTH, 4);

        em.persist(person.country);
        em.persist(person);

        person.birthDate.set(Calendar.YEAR, 1966);
        em.update(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class, "p");

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, person.firstName)
                        .and(personRoot.eq(Person_.lastName, person.lastName))
        );

        assertEquals(person, qb.build("p").getSingleResult());
    }


}
