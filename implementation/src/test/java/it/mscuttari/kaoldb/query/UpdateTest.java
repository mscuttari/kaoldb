package it.mscuttari.kaoldb.query;

import static org.hamcrest.junit.MatcherAssert.assertThat;

import androidx.test.core.app.ApplicationProvider;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import it.mscuttari.kaoldb.AbstractTest;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.mapping.DatabaseObject;

public class UpdateTest extends AbstractTest {

	/**
	 *           A
	 *           |
	 *     ------------
	 *     |          |
	 *     AA        AB
	 *     |
	 *  --------
	 *  |      |
	 * AAA    AAB
	 */

	@Entity
	@Table(name = "a")
	@DiscriminatorColumn(name = "type")
	private abstract static class A {

		@Id
		@Column(name = "id")
		public Integer id;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			A parent = (A) o;
			return id.equals(parent.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}

	@Entity
	@Table(name = "aa")
	@DiscriminatorColumn(name = "type")
	@DiscriminatorValue(value = "aa")
	private abstract static class AA extends A {

		@Column
		public Integer value;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			AA child1 = (AA) o;
			return Objects.equals(value, child1.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), value);
		}
	}

	@Entity
	@Table(name = "aaa")
	@DiscriminatorValue(value = "aaa")
	private static class AAA extends AA {

	}

	@Entity
	@Table(name = "aab")
	@DiscriminatorValue(value = "aab")
	private static class AAB extends AA {

	}

	@Entity
	@Table(name = "ab")
	@DiscriminatorValue(value = "ab")
	private static class AB extends A {

	}

	private EntityManager entityManager;

	@Before
	public void setUp() {
		LogUtils.enabled = true;

		DatabaseObject db = new DatabaseObject();
		db.setName("Test");
		db.setVersion(1);

		db.addEntityClass(A.class);
		db.addEntityClass(AA.class);
		db.addEntityClass(AAA.class);
		db.addEntityClass(AAB.class);
		db.addEntityClass(AB.class);

		db.mapEntities();
		db.waitUntilReady();

		entityManager = EntityManagerImpl.getEntityManager(ApplicationProvider.getApplicationContext(), db);
	}

	@After
	public void tearDown() {
		entityManager.deleteDatabase();
	}

	@Test
	public void sameClass() {
		AAA aaa = new AAA();
		aaa.id = 1;

		aaa.value = 10;
		entityManager.persist(aaa);
		aaa.value = 20;
		entityManager.update(aaa);

		List<AA> elements = entityManager.getAll(AA.class);
		assertThat(elements, Matchers.contains(aaa));
	}

	@Test
	public void brotherClass() {
		AA aa = new AAA();
		aa.id = 1;
		entityManager.persist(aa);

		aa = new AAB();
		aa.id = 1;
		entityManager.update(aa);

		List<AA> elements = entityManager.getAll(AA.class);
		assertThat(elements, Matchers.contains(aa));
	}

	@Test
	public void cousinClass() {
		A a = new AB();
		a.id = 1;
		entityManager.persist(a);

		a = new AAB();
		a.id = 1;
		entityManager.update(a);

		List<A> elements = entityManager.getAll(A.class);
		assertThat(elements, Matchers.contains(a));
	}

}
