package it.mscuttari.kaoldb.examples.generic;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import it.mscuttari.kaoldb.examples.generic.models.A;
import it.mscuttari.kaoldb.examples.generic.models.AA;
import it.mscuttari.kaoldb.examples.generic.models.AB;
import it.mscuttari.kaoldb.examples.generic.models.A_;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PersistTest extends AbstractGenericTest {

    @Test
    public void persistSingleChild() {
        AA child = new AA(0, 1, 10, 11);

        em.persist(child);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(1));
        assertEquals(child, results.get(0));
    }

    @Test
    public void persistSameClassChildren() {
        AA child1 = new AA(0, 1, 10, 11);
        AA child2 = new AA(1, 2, 20, 21);

        em.persist(child1);
        em.persist(child2);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(2));
        assertThat(results, containsInAnyOrder(child1, child2));
    }

    @Test
    public void persistDifferentClassChildren() {
        AA child1 = new AA(0, 1, 10, 11);
        AB child2 = new AB(1, 2, 20, 21);

        em.persist(child1);
        em.persist(child2);

        List<A> results = em.getAll(A.class);

        assertThat(results, hasSize(2));
        assertThat(results, containsInAnyOrder(child1, child2));
    }

    @Test
    public void persisteDifferentClassChildrenWithAutoIncrement() {
        AA child1 = new AA(null, 1, 10, 11);
        AB child2 = new AB(null, 2, 20, 21);

        em.persist(child1);
        em.persist(child2);

        {
            // Check child1
            List<AA> results = em.getAll(AA.class);

            assertThat(results, hasSize(1));
            assertThat(results.get(0).a2, equalTo(child1.a2));
            assertThat(results.get(0).aa1, equalTo(child1.aa1));
            assertThat(results.get(0).aa2, equalTo(child1.aa2));
        }

        {
            // Check child2
            List<AB> results = em.getAll(AB.class);

            assertThat(results, hasSize(1));
            assertThat(results.get(0).a2, equalTo(child2.a2));
            assertThat(results.get(0).ab1, equalTo(child2.ab1));
            assertThat(results.get(0).ab2, equalTo(child2.ab2));
        }
    }

}
