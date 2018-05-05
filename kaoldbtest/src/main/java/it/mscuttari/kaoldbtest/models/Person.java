package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "people")
public class Person {

    @Id
    @Column(name = "first_name")
    public String firstName;

    @Id
    @Column(name = "last_name")
    public String lastName;

}
