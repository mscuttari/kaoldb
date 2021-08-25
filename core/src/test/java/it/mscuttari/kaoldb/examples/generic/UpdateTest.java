package it.mscuttari.kaoldb.examples.generic;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import it.mscuttari.kaoldb.examples.generic.models.A;
import it.mscuttari.kaoldb.examples.generic.models.AA;
import it.mscuttari.kaoldb.examples.generic.models.AB;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class UpdateTest extends AbstractGenericTest {

    @Test
    public void updateSameClass() {
        AA entity = new AA(0, 1, 2, 10, 11);
        em.persist(entity);

        entity.aa1 = 12;
        entity.aa2 = 13;

        em.update(entity);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(1));
        assertThat(results, contains(entity));
    }

    @Test
    public void updateDifferentClass() {
        AA entity1 = new AA(0, 1, 2, 10, 11);
        em.persist(entity1);

        AB entity2 = new AB(0, 3, 4, 20, 21);
        em.update(entity2);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(1));
        assertThat(results, contains(entity2));
    }

    @Test
    public void updateSameClassWithMultipleChildren() {
        AA entity1 = new AA(0, 1, 2, 10, 11);
        AB entity2 = new AB(1, 3, 4, 20, 21);

        em.persist(entity1);
        em.persist(entity2);

        AA entity3 = new AA(0, 5, 6, 12, 13);

        em.update(entity3);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(2));
        assertThat(results, containsInAnyOrder(entity2, entity3));
    }

    @Test
    public void updateDifferentClassWithMultipleChildren() {
        AA entity1 = new AA(0, 1, 2, 10, 11);
        AB entity2 = new AB(1, 3, 4, 20, 21);

        em.persist(entity1);
        em.persist(entity2);

        AB entity3 = new AB(0, 5, 6, 22, 23);

        em.update(entity3);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(2));
        assertThat(results, containsInAnyOrder(entity2, entity3));
    }

}
