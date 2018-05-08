package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "thriller_films")
@DiscriminatorValue(value = "Thriller")
public final class ThrillerFilm extends Film {

}
