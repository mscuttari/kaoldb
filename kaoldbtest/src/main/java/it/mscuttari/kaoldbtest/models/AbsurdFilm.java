package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "absurd_film")
@DiscriminatorValue(value = "Absurd")
public class AbsurdFilm extends FantasyFilm {
}
