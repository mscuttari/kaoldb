package it.mscuttari.kaoldbtest;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static org.junit.Assert.assertTrue;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public abstract class AbstractTest {

    private final String databaseName;
    protected EntityManager em;


    /**
     * Constructor
     *
     * @param   databaseName    database name
     */
    public AbstractTest(String databaseName) {
        this.databaseName = databaseName;
    }


    /**
     * Create the database, load the configuration and get an entity manager instance
     */
    @Before
    public void setUp() {
        // KaolDB instance
        KaolDB kdb = KaolDB.getInstance();
        kdb.setDebugMode(true);
        kdb.setConfig(RuntimeEnvironment.application, R.xml.persistence);

        // Entity manager
        em = kdb.getEntityManager(RuntimeEnvironment.application, databaseName);
        em.deleteDatabase();
    }


    /**
     * Delete the database
     */
    @After
    public void tearDown() {
        assertTrue(em.deleteDatabase());
    }


    /**
     * Get a calendar with an already set date
     *
     * @param   year    year
     * @param   month   month number
     * @param   day     day of month
     *
     * @return  calendar
     */
    protected final Calendar getCalendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return calendar;
    }

}
