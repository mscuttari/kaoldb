package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @Column(name = "name")
    public String name;


    /**
     * Default constructor
     */
    public Genre() {
        this(null);
    }


    /**
     * Constructor
     *
     * @param   name    name
     */
    public Genre(String name) {
        this.name = name;
    }

}
