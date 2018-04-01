package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "fantasy_books")
@DiscriminatorValue(value = "1")
public class FantasyBook extends Book {

}
