package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "libraries")
public class Library {

    @Id
    @Column(name = "name")
    public String name;

    @Id
    @Column(name = "place")
    public String place;

    @Column(name = "ticket_price")
    public float ticketPrice;

    @JoinColumns(value = {
            @JoinColumn(name = "director_first_name", referencedColumnName = "first_name"),
            @JoinColumn(name = "director_last_name", referencedColumnName = "last_name")
    })
    public Person director;

}
