package it.mscuttari.kaoldb.mapping;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.AbstractTest;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ColumnsInheritanceTest extends AbstractTest {

	@Entity
	@Table(name = "grandparent")
	@DiscriminatorColumn(name = "type")
	private static class Grandparent {

		@Id
		@Column(name = "id_grandparent")
		public Long idGrandparent;

	}

	@Entity
	@Table(name = "parent")
	@DiscriminatorColumn(name = "type")
	@DiscriminatorValue(value = "parent")
	private static class Parent extends Grandparent {

		@Id
		@Column(name = "id_parent")
		public Long idParent;

		@Column(name = "parent_field")
		public String parentField;

	}

	@Entity
	@Table(name = "child")
	@DiscriminatorValue(value = "child")
	private static class Child extends Parent {

		@Column(name = "child_field")
		public String childField;

	}

	private DatabaseObject db;

	@Before
	public void setUp() {
		LogUtils.enabled = true;
		db = new DatabaseObject();
		db.setName("Test");

		db.addEntityClass(Grandparent.class);
		db.addEntityClass(Parent.class);
		db.addEntityClass(Child.class);

		db.mapEntities();
		db.waitUntilReady();
	}

	@Test
	public void parentKeysInherited() {
		EntityObject<Child> child = db.getEntity(Child.class);

		Collection<String> primaryKeys = child.columns
				.getPrimaryKeys()
				.stream()
				.map(key -> key.name)
				.collect(Collectors.toList());

		assertThat(primaryKeys, Matchers.hasItem("id_parent"));
	}

	@Test
	public void grandparentKeysInherited() {
		EntityObject<Child> child = db.getEntity(Child.class);

		Collection<String> primaryKeys = child.columns
				.getPrimaryKeys()
				.stream()
				.map(key -> key.name)
				.collect(Collectors.toList());

		assertThat(primaryKeys, Matchers.hasItem("id_grandparent"));
	}

	@Test
	public void parentNormalColumnsNotInherited() {
		EntityObject<Child> child = db.getEntity(Child.class);
		assertFalse(child.columns.contains("parent_field"));
	}

}
