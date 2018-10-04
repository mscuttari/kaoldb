package it.mscuttari.kaoldbtest.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @Column(name = "name")
    public String name;

    @OneToMany(mappedBy = "genre")
    public Collection<Film> films;


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
