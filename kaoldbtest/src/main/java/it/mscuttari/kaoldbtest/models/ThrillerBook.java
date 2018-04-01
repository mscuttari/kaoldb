package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "thriller_books")
@DiscriminatorValue(value = "2")
public class ThrillerBook extends Book {

}
