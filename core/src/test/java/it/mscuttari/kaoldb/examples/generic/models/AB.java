package it.mscuttari.kaoldb.examples.generic.models;

import java.util.Objects;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "AB")
@DiscriminatorValue(value = "AB")
public class AB extends A {

    @Column(name = "AB1")
    public Integer ab1;

    @Column(name = "AB2")
    public Integer ab2;

    public AB() {

    }

    public AB(Integer a1, Integer a2, Integer ab1, Integer ab2) {
        super(a1, a2);
        this.ab1 = ab1;
        this.ab2 = ab2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AB ab = (AB) o;
        return Objects.equals(ab1, ab.ab1) &&
                Objects.equals(ab2, ab.ab2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ab1, ab2);
    }

}
