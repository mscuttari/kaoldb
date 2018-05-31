package it.mscuttari.kaoldbtest.models;

import java.util.Arrays;

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


    /**
     * Default constructor
     */
    public Person() {
        this(null, null);
    }


    /**
     * Constructor
     *
     * @param   firstName       first name
     * @param   lastName        last name
     */
    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }


    @Override
    public int hashCode() {
        Object[] x = {firstName, lastName};
        return Arrays.hashCode(x);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof Person)) return false;
        Person o = (Person) obj;

        if (firstName != null && !firstName.equals(o.firstName)) return false;
        if (firstName == null && o.firstName != null) return false;

        if (lastName != null && !lastName.equals(o.lastName)) return false;
        if (lastName == null && o.lastName != null) return false;

        return true;
    }

}
