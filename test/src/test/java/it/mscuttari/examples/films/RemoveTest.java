/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.examples.films;

import org.junit.Test;

import java.util.Calendar;

import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertNull;

public class RemoveTest extends AbstractFilmTest {

    @Test
    public void removePerson() {
        Person person = new Person("Robert", "Downey Jr.", getCalendar(1965, Calendar.APRIL, 4), new Country("USA"));

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
