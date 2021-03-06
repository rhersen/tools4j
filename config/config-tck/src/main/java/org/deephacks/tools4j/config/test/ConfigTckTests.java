/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.config.test;

import static org.deephacks.tools4j.config.model.Events.CFG101;
import static org.deephacks.tools4j.config.model.Events.CFG105;
import static org.deephacks.tools4j.config.model.Events.CFG106;
import static org.deephacks.tools4j.config.model.Events.CFG110;
import static org.deephacks.tools4j.config.model.Events.CFG111;
import static org.deephacks.tools4j.config.model.Events.CFG301;
import static org.deephacks.tools4j.config.model.Events.CFG302;
import static org.deephacks.tools4j.config.model.Events.CFG304;
import static org.deephacks.tools4j.config.model.Events.CFG307;
import static org.deephacks.tools4j.config.model.Events.CFG308;
import static org.deephacks.tools4j.config.model.Events.CFG309;
import static org.deephacks.tools4j.config.test.BeanUnitils.toBean;
import static org.deephacks.tools4j.config.test.BeanUnitils.toBeans;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.test.ConfigTestData.Grandfather;
import org.deephacks.tools4j.config.test.ConfigTestData.Person;
import org.deephacks.tools4j.config.test.ConfigTestData.Singleton;
import org.deephacks.tools4j.config.test.ConfigTestData.SingletonParent;
import org.deephacks.tools4j.support.event.AbortRuntimeException;
import org.junit.Before;
import org.junit.Test;

/**
 * A funcational set of end-to-end tests for running compatibility tests.
 * 
 * Theses tests are intended to be easily reused as a test suite for simplifying
 * testing compatibility of many different combinations of service providers 
 * and configurations.
 * 
 * It is the responsibility of subclasses to initalize the lookup of
 * service providers and their behaviour.
 * 
 */
public abstract class ConfigTckTests extends ConfigDefaultSetup {
    /**
     * This method can be used to do initalize tests in the subclass 
     * before the superclass.
     */
    public abstract void before();

    @Before
    public final void beforeMethod() {
        before();
        setupDefaultConfigData();
    }

    @Test
    public void test_create_set_merge_non_existing_property() {
        createDefault();
        Bean bean = Bean.create(c1.getId());
        bean.addProperty("non_existing", "bogus");
        try {
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            bean = Bean.create(BeanId.create("c5", ConfigTestData.CHILD_SCHEMA_NAME));
            bean.setReference("non_existing", c1.getId());
            admin.create(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG111));
        }
        bean = Bean.create(c1.getId());
        bean.addProperty("non_existing", "bogus");

        try {
            admin.set(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
        try {
            admin.merge(bean);
            fail("Not possible to set property names that does not exist in schema");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG110));
        }
    }

    /**
     * Test the possibility for:
     *  
     * 1) Creating individual beans that have references to eachother.
     * 2) Created beans can be fetched individually.
     * 3) That the runtime view sees the same result.
     */
    @Test
    public void test_create_single_then_get_list() {
        createThenGet(c1);
        createThenGet(c2);
        listAndAssert(c1.getId().getSchemaName(), c1, c2);
        createThenGet(p1);
        createThenGet(p2);
        listAndAssert(p1.getId().getSchemaName(), p1, p2);
        createThenGet(g1);
        createThenGet(g2);
        listAndAssert(g1.getId().getSchemaName(), g1, g2);
    }

    /**
     * Test the possibility for:
     * 
     * 1) Creating a collection of beans that have references to eachother.
     * 2) Created beans can be fetched individually afterwards.
     * 3) Created beans can be listed afterwards.
     * 4) That the runtime view sees the same result as admin view. 
     */
    @Test
    public void test_create_multiple_then_get_list() {
        createDefault();
        getAndAssert(c1);
        getAndAssert(c2);
        listAndAssert(c1.getId().getSchemaName(), c1, c2);
        getAndAssert(p1);
        getAndAssert(p2);
        listAndAssert(p1.getId().getSchemaName(), p1, p2);
        getAndAssert(g1);
        getAndAssert(g2);
        listAndAssert(g1.getId().getSchemaName(), g1, g2);

    }

    /**
     * Test that singleton beans have their default instance created after registration.
     */
    @Test
    public void test_register_singleton() {
        Singleton singleton = runtime.singleton(Singleton.class);
        assertNotNull(singleton);
    }

    /**
     * Test that singleton beans cannot be deleted.
     */
    @Test
    public void test_delete_singleton() {
        try {
            admin.delete(toBean(s1).getId());
            fail("Should not be possible to delete singleton instances.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG307));
        }
    }

    /**
     * Test that it is not possible to create beans of singleton type. 
     */
    @Test
    public void test_create_singleton() {
        Bean b = Bean.create(BeanId.create("somethingElse", ConfigTestData.SINGLETON_SCHEMA_NAME));
        try {
            admin.create(b);
            fail("Should not be possible to create additional singleton instances.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG308));
        }
    }

    /**
     * Test that singleton references are automatically assigned without needing to provision them 
     * from admin context.
     */
    @Test
    public void test_singleton_references() {
        // provision a bean without the singleton reference.
        Bean singletonParent = toBean(sp1);
        admin.create(singletonParent);

        // asert that the singleton reference is set for runtime
        SingletonParent parent = runtime.get(singletonParent.getId().getInstanceId(),
                SingletonParent.class);
        assertNotNull(parent.singleton);

        // assert that the singleton reference is set for admin
        Bean result = admin.get(singletonParent.getId());
        BeanId singletonId = result.getFirstReference("singleton");
        assertThat(singletonId, is(s1.getBeanId()));
        assertThat(singletonId.getBean(), is(toBean(s1)));

    }

    /**
     * Test the possibility for:
     * 
     * 1) Setting an empty bean that will erase properties and references.
     * 3) Bean that was set empty can be fetched individually.
     * 4) That the runtime view sees the same result as admin view. 
     */
    @Test
    public void test_set_get_single() {
        createDefault();
        Grandfather empty = testdata.getEmptyGrandfather("g1");
        Bean empty_expect = toBean(empty);

        admin.set(empty_expect);
        Bean empty_result = admin.get(empty.getId());
        assertReflectionEquals(empty_expect, empty_result, LENIENT_ORDER);
    }

    @Test
    public void test_set_get_list() {
        createDefault();
        Grandfather empty_g1 = testdata.getEmptyGrandfather("g1");
        Grandfather empty_g2 = testdata.getEmptyGrandfather("g2");
        Collection<Bean> empty_expect = toBeans(empty_g1, empty_g2);
        admin.set(empty_expect);
        Collection<Bean> empty_result = admin.list(empty_g1.getId().getSchemaName());
        assertReflectionEquals(empty_expect, empty_result, LENIENT_ORDER);
        runtimeAllAndAssert(empty_g1.getClass(), empty_g1, empty_g2);
    }

    @Test
    public void test_merge_get_single() {
        createDefault();

        Grandfather merged = testdata.getEmptyGrandfather("g1");
        merged.prop14 = TimeUnit.NANOSECONDS;
        merged.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
        merged.prop1 = "newName";

        Bean mergeBean = toBean(merged);
        admin.merge(mergeBean);

        // modify the original to fit the expected merge 
        g1.prop1 = merged.prop1;
        g1.prop19 = merged.prop19;
        g1.prop14 = merged.prop14;
        getAndAssert(g1);
    }

    @Test
    public void test_merge_get_list() {
        createDefault();

        Grandfather g1_merged = testdata.getEmptyGrandfather("g1");
        g1_merged.prop14 = TimeUnit.NANOSECONDS;
        g1_merged.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
        g1_merged.prop1 = "newName";

        Grandfather g2_merged = testdata.getEmptyGrandfather("g2");
        g2_merged.prop14 = TimeUnit.NANOSECONDS;
        g2_merged.prop19 = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS);
        g2_merged.prop1 = "newName";

        Collection<Bean> mergeBeans = toBeans(g1_merged, g2_merged);
        admin.merge(mergeBeans);

        // modify the original to fit the expected merge 
        g1.prop1 = g1_merged.prop1;
        g1.prop19 = g1_merged.prop19;
        g1.prop14 = g1_merged.prop14;

        g2.prop1 = g2_merged.prop1;
        g2.prop19 = g2_merged.prop19;
        g2.prop14 = g2_merged.prop14;

        listAndAssert(g1.getId().getSchemaName(), g1, g2);

    }

    @Test
    public void test_merge_and_set_broken_references() {
        createDefault();
        // try merge a invalid single reference
        Bean b = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try merge a invalid reference on collection
        b = Bean.create(BeanId.create("p2", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop7", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.merge(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try set a invalid single reference
        b = Bean.create(BeanId.create("parent4", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.set(b);
            fail("Should not be possible to merge beans that does not exist");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                fail("Should not be possible to merge invalid reference");
            }
        }

        // try merge a invalid single reference
        b = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        b.addReference("prop6", BeanId.create("non_existing_child_ref", ""));
        try {
            admin.set(b);
            fail("Should not be possible to merge invalid reference");
        } catch (AbortRuntimeException e) {
            if (e.getEvent().getCode() != CFG301 && e.getEvent().getCode() != CFG304) {
                fail("Should not be possible to merge invalid reference");
            }
        }

    }

    @Test
    public void test_delete_bean() {
        createDefault();

        admin.delete(g1.getId());

        try {
            admin.get(g1.getId());
            fail("Bean should have been deleted");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG304));
        }
    }

    @Test
    public void test_delete_beans() {
        createDefault();

        admin.delete(g1.getId().getSchemaName(), Arrays.asList("g1", "g2"));

        List<Bean> result = admin.list(g1.getId().getSchemaName());
        assertThat(result.size(), is(0));
    }

    @Test
    public void test_delete_reference_violation() {
        admin.create(toBeans(g1, g2, p1, p2, c1, c2));
        // test single
        try {
            admin.delete(BeanId.create("c1", ConfigTestData.CHILD_SCHEMA_NAME));
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
        }
        // test multiple
        try {
            admin.delete(ConfigTestData.CHILD_SCHEMA_NAME, Arrays.asList("c1", "c2"));
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG302));
        }
    }

    @Test
    public void test_set_merge_without_schema() {
        Bean b = Bean.create(BeanId.create("1", "missing_schema_name"));
        try {
            admin.create(b);
            fail("Cant add beans without a schema.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
        }
        try {
            admin.merge(b);
            fail("Cant add beans without a schema.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG101));
        }
    }

    @Test
    public void test_set_merge_violating_types() {
        admin.create(toBeans(g1, g2, p1, p2, c1, c2));

        Bean child = Bean.create(BeanId.create("c1", ConfigTestData.CHILD_SCHEMA_NAME));
        // child merge invalid byte
        try {
            child.setProperty("prop8", "100000");
            admin.set(child);
            fail("10000 does not fit java.lang.Byte");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // child merge invalid integer
        try {
            child.addProperty("prop3", "2.2");
            admin.merge(child);
            fail("2.2 does not fit a collection of java.lang.Integer");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // parent set invalid enum value 
        Bean parent = Bean.create(BeanId.create("g1", ConfigTestData.GRANDFATHER_SCHEMA_NAME));
        try {
            parent.setProperty("prop14", "not_a_enum");
            admin.set(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }
        // parent merge invalid value to enum list
        parent = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        try {
            parent.addProperty("prop19", "not_a_enum");
            admin.merge(parent);
            fail("not_a_enum is not a value of TimeUnit");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }

        // grandfather merge invalid multiplicity type, i.e. single on multi value.
        Bean grandfather = Bean.create(BeanId.create("g1", ConfigTestData.GRANDFATHER_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop1", Arrays.asList("1", "2"));
            admin.merge(grandfather);
            fail("Cannot add mutiple values to a single valued property.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG106));
        }

        // grandfather set invalid multiplicity type, multi value on single.
        grandfather = Bean.create(BeanId.create("p1", ConfigTestData.PARENT_SCHEMA_NAME));
        try {
            grandfather.addProperty("prop11", "2.0");
            admin.set(parent);
            fail("Cannot add a value to a single typed value.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG105));
        }

    }

    @Test
    public void test_circular_references() {
        String personSchema = "person";
        runtime.register(Person.class);

        BeanId aId = BeanId.create("a", personSchema);
        BeanId bId = BeanId.create("b", personSchema);
        BeanId cId = BeanId.create("c", personSchema);
        BeanId dId = BeanId.create("d", personSchema);

        Bean a = Bean.create(aId);
        Bean b = Bean.create(bId);
        Bean c = Bean.create(cId);
        Bean d = Bean.create(dId);

        admin.create(Arrays.asList(a, b, c, d));

        a.setReference("bestFriend", bId);
        b.setReference("bestFriend", aId);
        c.setReference("bestFriend", dId);
        d.setReference("bestFriend", cId);

        a.addReference("closeFriends", Arrays.asList(bId, cId, dId));
        b.addReference("closeFriends", Arrays.asList(aId, cId, dId));
        c.addReference("closeFriends", Arrays.asList(aId, bId, dId));
        d.addReference("closeFriends", Arrays.asList(aId, bId, cId));

        a.addReference("colleauges", Arrays.asList(bId, cId, dId));
        b.addReference("colleauges", Arrays.asList(aId, cId, dId));
        c.addReference("colleauges", Arrays.asList(aId, bId, dId));
        d.addReference("colleauges", Arrays.asList(aId, bId, cId));
        /**
         * Now test all operations from admin and runtime to make 
         * sure that none of them get stuck in infinite recrusion. 
         */

        admin.merge(Arrays.asList(a, b, c, d));
        admin.set(Arrays.asList(a, b, c, d));
        admin.list("person");
        admin.get(BeanId.create("b", "person"));
        runtime.all(Person.class);
        runtime.get("c", Person.class);

    }

    @Test
    public void test_JSR303_validation_success() {
        jsr303.prop = "Valid upper value for @FirstUpperValidator";
        jsr303.width = 2;
        jsr303.height = 2;
        Bean jsr303Bean = toBean(jsr303);
        admin.create(jsr303Bean);
    }

    @Test
    public void test_JSR303_validation_failures() {
        jsr303.prop = "Valid upper value for @FirstUpperValidator";
        jsr303.width = 20;
        jsr303.height = 20;
        Bean jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Area exceeds constraint");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.prop = "test";
        jsr303.width = 1;
        jsr303.height = 1;
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop does not have first upper case.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }
        jsr303.prop = "T";
        jsr303.width = 1;
        jsr303.height = 1;
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Prop must be longer than one char");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

        jsr303.prop = "Valid upper value for @FirstUpperValidator";
        jsr303.width = null;
        jsr303.height = null;
        jsr303Bean = toBean(jsr303);
        try {
            admin.create(jsr303Bean);
            fail("Width and height may not be null.");
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(CFG309));
        }

    }

    private void createThenGet(Object object) throws AssertionFailedError {
        Bean bean = toBean(object);
        admin.create(bean);
        getAndAssert(object);
    }

    private void getAndAssert(Object object) throws AssertionFailedError {
        Bean bean = toBean(object);
        Bean result = admin.get(bean.getId());
        assertReflectionEquals(bean, result, LENIENT_ORDER);
        runtimeGetAndAssert(object, bean);
    }

    /**
     * Create the default testdata structure. 
     */
    private void createDefault() {
        admin.create(defaultBeans);

    }

    private void listAndAssert(String schemaName, Object... objects) {
        Collection<Bean> beans = admin.list(schemaName);
        assertReflectionEquals(toBeans(objects), beans, LENIENT_ORDER);
        runtimeAllAndAssert(objects[0].getClass(), objects);
    }

    private void runtimeGetAndAssert(Object object, Bean bean) throws AssertionFailedError {
        Object o = runtime.get(bean.getId().getInstanceId(), object.getClass());
        assertReflectionEquals(object, o, LENIENT_ORDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void runtimeAllAndAssert(Class clazz, Object... objects) throws AssertionFailedError {
        List<Object> reslut = runtime.all(clazz);
        assertReflectionEquals(objects, reslut, LENIENT_ORDER);
    }
}
