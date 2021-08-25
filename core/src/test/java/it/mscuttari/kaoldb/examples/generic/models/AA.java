package it.mscuttari.kaoldb.examples.generic.models;

import java.util.Objects;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "AA")
@DiscriminatorValue(value = "AA")
public class AA extends A {

    @Column(name = "AA1")
    public Integer aa1;

    @Column(name = "AA2")
    public Integer aa2;

    public AA() {

    }

    public AA(Integer pk, Integer a1, Integer a2, Integer aa1, Integer aa2) {
        super(pk, a1, a2);
        this.aa1 = aa1;
        this.aa2 = aa2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AA aa = (AA) o;
        return Objects.equals(aa1, aa.aa1) &&
                Objects.equals(aa2, aa.aa2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), aa1, aa2);
    }

}
