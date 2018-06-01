package it.mscuttari.kaoldbtest.models;

import java.util.Arrays;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "countries")
public class Country {

    @Id
    @Column(name = "name")
    private String name;


    /**
     * Default constructor
     */
    public Country() {
        this(null);
    }


    /**
     * Constructor
     *
     * @param   name    name
     */
    public Country(String name) {
        this.name = name;
    }


    @Override
    public int hashCode() {
        Object[] x = {name};
        return Arrays.hashCode(x);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof Country)) return false;
        Country o = (Country) obj;

        if (name != null && !name.equals(o.name)) return false;
        if (name == null && o.name != null) return false;

        return true;
    }


    @Override
    public String toString() {
        return "[name: " + name + "]";
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

}
