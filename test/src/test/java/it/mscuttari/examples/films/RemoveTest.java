package it.mscuttari.examples.films;

import org.junit.Test;

import java.util.Calendar;

import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertNull;

public class RemoveTest extends AbstractFilmTest {

    @Test
    public void removePerson() {
        Person person = new Person(
                "Robert",
                "Downey Jr.",
                getCalendar(1965, Calendar.APRIL, 4),
                new Country("USA")
        );

        em.persist(person.country);
        em.persist(person);
        em.remove(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class);

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, person.firstName)
                        .and(personRoot.eq(Person_.lastName, person.lastName))
        );

        assertNull(qb.build(personRoot).getSingleResult());
    }

}
