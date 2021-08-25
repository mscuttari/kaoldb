package it.mscuttari.kaoldb.examples.generic;

import org.junit.Test;

import java.util.List;

import it.mscuttari.kaoldb.examples.generic.models.A;
import it.mscuttari.kaoldb.examples.generic.models.AA;
import it.mscuttari.kaoldb.examples.generic.models.AB;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RemoveTest extends AbstractGenericTest {

    @Test
    public void removeUniqueEntry() {
        AA entity = new AA(0, 1, 2, 10, 11);

        em.persist(entity);
        em.remove(entity);

        List<A> results = em.getAll(A.class);

        assertThat(results, empty());
    }

    @Test
    public void removeInterleavedEntry() {
        AA entity1 = new AA(0, 1, 2, 10, 11);
        AA entity2 = new AA(1, 2, 3, 12, 13);
        AB entity3 = new AB(2, 4, 5, 20, 21);

        em.persist(entity1);
        em.persist(entity2);
        em.persist(entity3);

        em.remove(entity1);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(2));
        assertThat(results, containsInAnyOrder(entity2, entity3));
    }

}
