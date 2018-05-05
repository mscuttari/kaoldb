package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "fantasy_films")
@DiscriminatorValue(value = "Fantasy")
public class FantasyFilm extends Film {

}
