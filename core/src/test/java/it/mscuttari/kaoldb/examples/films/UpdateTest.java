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

package it.mscuttari.kaoldb.examples.films;

import org.junit.Test;

import java.util.Calendar;

import it.mscuttari.kaoldb.examples.films.models.Country;
import it.mscuttari.kaoldb.examples.films.models.FantasyFilm;
import it.mscuttari.kaoldb.examples.films.models.Film;
import it.mscuttari.kaoldb.examples.films.models.FilmRestriction;
import it.mscuttari.kaoldb.examples.films.models.Film_;
import it.mscuttari.kaoldb.examples.films.models.Person;
import it.mscuttari.kaoldb.examples.films.models.Person_;
import it.mscuttari.kaoldb.examples.films.models.ThrillerFilm;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertEquals;

public class UpdateTest extends AbstractFilmTest {

    @Test
    public void updatePerson() {
        Person person = new Person("Robert", "Downey Jr.", getCalendar(1965, Calendar.APRIL, 4), new Country("USA"));

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

    @Test
    public void updateFilm() {
        FantasyFilm film = new FantasyFilm("Test", 2020, null, 160, null);
        film.test = "AAA";

        em.persist(film.genre);
        em.persist(film);

        film.test = "BBB";
        em.update(film);

        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> root = qb.getRoot(Film.class);

        Query<Film> query = qb.from(root).where(root.eq(Film_.title, "Test")).build(root);
        Film result = query.getSingleResult();
        assertEquals(film, result);
    }

    @Test
    public void changeFilm() {
        Film film = new FantasyFilm("Test", 2020, null, 160, null);

        em.persist(film.genre);
        em.persist(film);

        film = new ThrillerFilm("Test", 2020, null, 160, FilmRestriction.MIN14);
        em.persist(film.genre);
        em.update(film);

        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> root = qb.getRoot(Film.class);

        Query<Film> query = qb.from(root).where(root.eq(Film_.title, "Test")).build(root);
        Film result = query.getSingleResult();
        assertEquals(film, result);
    }

}
