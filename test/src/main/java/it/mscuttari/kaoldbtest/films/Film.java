package it.mscuttari.kaoldbtest.films;

import java.util.Arrays;
import java.util.Collection;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorType;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "films")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "genre", discriminatorType = DiscriminatorType.STRING)
public abstract class Film {

    @Id
    @Column(name = "title")
    public String title;

    @Id
    @Column(name = "year")
    public Integer year;

    @ManyToOne
    @JoinColumn(name = "genre", referencedColumnName = "name")
    public Genre genre;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "director_first_name", referencedColumnName = "first_name"),
            @JoinColumn(name = "director_last_name", referencedColumnName = "last_name")
    })
    public Person director;

    @ManyToMany
    @JoinTable(
            name = "acting",
            joinColumns = {
                    @JoinColumn(name = "film_title", referencedColumnName = "title"),
                    @JoinColumn(name = "film_year", referencedColumnName = "year")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "actor_first_name", referencedColumnName = "first_name"),
                    @JoinColumn(name = "actor_last_name", referencedColumnName = "last_name")
            }
            )
    public Collection<Person> actors;
    
    @Column(name = "length")
    public Integer length;

    @Column(name = "restriction")
    public FilmRestriction restriction;


    /**
     * Constructor
     *
     * @param   title           title
     * @param   year            year
     * @param   genre           genre
     * @param   director        director
     * @param   length          length
     * @param   restriction     restriction
     */
    public Film(String title, Integer year, Genre genre, Person director, Integer length, FilmRestriction restriction) {
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.director = director;
        this.length = length;
        this.restriction = restriction;
    }


    @Override
    public int hashCode() {
        Object[] x = {title, year};
        return Arrays.hashCode(x);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof Film)) return false;
        Film o = (Film) obj;

        if (title != null && !title.equals(o.title)) return false;
        if (title == null && o.title != null) return false;

        if (year != null && !year.equals(o.year)) return false;
        if (year == null && o.year != null) return false;

        if (genre != null && !genre.equals(o.genre)) return false;
        if (genre == null && o.genre != null) return false;

        if (director != null && !director.equals(o.director)) return false;
        if (director == null && o.director != null) return false;

        if (length != null && !length.equals(o.length)) return false;
        if (length == null && o.length != null) return false;

        if (restriction != null && !restriction.equals(o.restriction)) return false;
        if (restriction == null && o.restriction != null) return false;

        return true;
    }


    @Override
    public String toString() {
        return "[title: " + title + ", year: " + year + "]";
    }

}
