package it.mscuttari.kaoldb.examples.generic.models;

import java.util.Objects;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorType;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "A")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class A {

    @Id
    @Column(name = "A1")
    public Integer a1;

    @Column(name = "A2")
    public Integer a2;

    public A() {

    }

    public A(Integer a1, Integer a2) {
        this.a1 = a1;
        this.a2 = a2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        A a = (A) o;
        return Objects.equals(a1, a.a1) &&
                Objects.equals(a2, a.a2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a1, a2);
    }

}
