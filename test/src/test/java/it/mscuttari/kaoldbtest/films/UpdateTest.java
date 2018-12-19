package it.mscuttari.kaoldbtest.films;

import org.junit.Test;

import java.util.Calendar;

import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertEquals;

public class UpdateTest extends AbstractFilmTest {

    @Test
    public void updatePerson() {
        Person person = new Person(
                "Robert",
                "Downey Jr.",
                getCalendar(1965, Calendar.APRIL, 4),
                new Country("USA")
        );

        em.persist(person.country);
        em.persist(person);

        person.birthDate.set(Calendar.YEAR, 1966);
        em.update(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class);

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, person.firstName)
                        .and(personRoot.eq(Person_.lastName, person.lastName))
        );

        assertEquals(person, qb.build(personRoot).getSingleResult());
    }

}
