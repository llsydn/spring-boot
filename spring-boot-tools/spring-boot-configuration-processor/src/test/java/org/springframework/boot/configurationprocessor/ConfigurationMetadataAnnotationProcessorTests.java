/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.TestJsonConverter;
import org.springframework.boot.configurationsample.incremental.BarProperties;
import org.springframework.boot.configurationsample.incremental.FooProperties;
import org.springframework.boot.configurationsample.incremental.RenamedBarProperties;
import org.springframework.boot.configurationsample.lombok.LombokExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokInnerClassProperties;
import org.springframework.boot.configurationsample.lombok.LombokInnerClassWithGetterProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleProperties;
import org.springframework.boot.configurationsample.lombok.SimpleLombokPojo;
import org.springframework.boot.configurationsample.method.DeprecatedMethodConfig;
import org.springframework.boot.configurationsample.method.EmptyTypeMethodConfig;
import org.springframework.boot.configurationsample.method.InvalidMethodConfig;
import org.springframework.boot.configurationsample.method.MethodAndClassConfig;
import org.springframework.boot.configurationsample.method.SimpleMethodConfig;
import org.springframework.boot.configurationsample.simple.ClassWithNestedProperties;
import org.springframework.boot.configurationsample.simple.DeprecatedSingleProperty;
import org.springframework.boot.configurationsample.simple.HierarchicalProperties;
import org.springframework.boot.configurationsample.simple.NotAnnotated;
import org.springframework.boot.configurationsample.simple.SimpleCollectionProperties;
import org.springframework.boot.configurationsample.simple.SimplePrefixValueProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleTypeProperties;
import org.springframework.boot.configurationsample.specific.BoxingPojo;
import org.springframework.boot.configurationsample.specific.BuilderPojo;
import org.springframework.boot.configurationsample.specific.DeprecatedUnrelatedMethodPojo;
import org.springframework.boot.configurationsample.specific.DoubleRegistrationProperties;
import org.springframework.boot.configurationsample.specific.ExcludedTypesPojo;
import org.springframework.boot.configurationsample.specific.GenericConfig;
import org.springframework.boot.configurationsample.specific.InnerClassAnnotatedGetterConfig;
import org.springframework.boot.configurationsample.specific.InnerClassProperties;
import org.springframework.boot.configurationsample.specific.InnerClassRootConfig;
import org.springframework.boot.configurationsample.specific.InvalidAccessorProperties;
import org.springframework.boot.configurationsample.specific.InvalidDoubleRegistrationProperties;
import org.springframework.boot.configurationsample.specific.SimplePojo;
import org.springframework.boot.junit.compiler.TestCompiler;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 */
public class ConfigurationMetadataAnnotationProcessorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	@Test
	public void notAnnotated() throws Exception {
		ConfigurationMetadata metadata = compile(NotAnnotated.class);
		assertThat(metadata.getItems()).isEmpty();
	}

	@Test
	public void simpleProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata)
				.has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class)
				.withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class)
				.fromSource(SimpleProperties.class).withDescription("A simple flag.")
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("simple.counter"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("simple.size"));
	}

	@Test
	public void simplePrefixValueProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimplePrefixValueProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple")
				.fromSource(SimplePrefixValueProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple.name", String.class)
				.fromSource(SimplePrefixValueProperties.class));
	}

	@Test
	public void simpleTypeProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleTypeProperties.class);
		assertThat(metadata).has(
				Metadata.withGroup("simple.type").fromSource(SimpleTypeProperties.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-string", String.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-byte", Byte.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-byte", Byte.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-char", Character.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-char", Character.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-boolean", Boolean.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-boolean", Boolean.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-short", Short.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-short", Short.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-integer", Integer.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-integer", Integer.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-long", Long.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-long", Long.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-double", Double.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-double", Double.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-float", Float.class));
		assertThat(metadata).has(
				Metadata.withProperty("simple.type.my-primitive-float", Float.class));
		assertThat(metadata.getItems().size()).isEqualTo(18);
	}

	@Test
	public void hierarchicalProperties() throws Exception {
		ConfigurationMetadata metadata = compile(HierarchicalProperties.class);
		assertThat(metadata).has(Metadata.withGroup("hierarchical")
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.first", String.class)
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata)
				.has(Metadata.withProperty("hierarchical.second", String.class)
						.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.third", String.class)
				.fromSource(HierarchicalProperties.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedProperties() throws Exception {
		Class<?> type = org.springframework.boot.configurationsample.simple.DeprecatedProperties.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("deprecated").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("deprecated.name", String.class)
				.fromSource(type).withDeprecation(null, null));
		assertThat(metadata)
				.has(Metadata.withProperty("deprecated.description", String.class)
						.fromSource(type).withDeprecation(null, null));
	}

	@Test
	public void singleDeprecatedProperty() throws Exception {
		Class<?> type = DeprecatedSingleProperty.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("singledeprecated").fromSource(type));
		assertThat(metadata)
				.has(Metadata.withProperty("singledeprecated.new-name", String.class)
						.fromSource(type));
		assertThat(metadata).has(Metadata
				.withProperty("singledeprecated.name", String.class).fromSource(type)
				.withDeprecation("renamed", "singledeprecated.new-name"));
	}

	@Test
	public void deprecatedOnUnrelatedSetter() throws Exception {
		Class<?> type = DeprecatedUnrelatedMethodPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
		assertThat(metadata)
				.has(Metadata.withProperty("not.deprecated.counter", Integer.class)
						.withNoDeprecation().fromSource(type));
		assertThat(metadata)
				.has(Metadata.withProperty("not.deprecated.flag", Boolean.class)
						.withNoDeprecation().fromSource(type));
	}

	@Test
	public void boxingOnSetter() throws IOException {
		Class<?> type = BoxingPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("boxing").fromSource(type));
		assertThat(metadata).has(
				Metadata.withProperty("boxing.flag", Boolean.class).fromSource(type));
		assertThat(metadata).has(
				Metadata.withProperty("boxing.counter", Integer.class).fromSource(type));
	}

	@Test
	public void parseCollectionConfig() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleCollectionProperties.class);
		// getter and setter
		assertThat(metadata).has(Metadata.withProperty("collection.integers-to-names",
				"java.util.Map<java.lang.Integer,java.lang.String>"));
		assertThat(metadata).has(Metadata.withProperty("collection.longs",
				"java.util.Collection<java.lang.Long>"));
		assertThat(metadata).has(Metadata.withProperty("collection.floats",
				"java.util.List<java.lang.Float>"));
		// getter only
		assertThat(metadata).has(Metadata.withProperty("collection.names-to-integers",
				"java.util.Map<java.lang.String,java.lang.Integer>"));
		assertThat(metadata).has(Metadata.withProperty("collection.bytes",
				"java.util.Collection<java.lang.Byte>"));
		assertThat(metadata).has(Metadata.withProperty("collection.doubles",
				"java.util.List<java.lang.Double>"));
	}

	@Test
	public void simpleMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleMethodConfig.class);
		assertThat(metadata)
				.has(Metadata.withGroup("foo").fromSource(SimpleMethodConfig.class));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(SimpleMethodConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.fromSource(SimpleMethodConfig.Foo.class));
	}

	@Test
	public void invalidMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InvalidMethodConfig.class);
		assertThat(metadata).has(Metadata.withProperty("something.name", String.class)
				.fromSource(InvalidMethodConfig.class));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("invalid.name"));
	}

	@Test
	public void methodAndClassConfig() throws Exception {
		ConfigurationMetadata metadata = compile(MethodAndClassConfig.class);
		assertThat(metadata).has(Metadata.withProperty("conflict.name", String.class)
				.fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("conflict.flag", Boolean.class)
				.fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("conflict.value", String.class)
				.fromSource(MethodAndClassConfig.class));
	}

	@Test
	public void emptyTypeMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(EmptyTypeMethodConfig.class);
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("something.foo"));
	}

	@Test
	public void deprecatedMethodConfig() throws Exception {
		Class<DeprecatedMethodConfig> type = DeprecatedMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(DeprecatedMethodConfig.Foo.class)
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.fromSource(DeprecatedMethodConfig.Foo.class)
				.withDeprecation(null, null));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedMethodConfigOnClass() throws Exception {
		Class<?> type = org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(
						org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.fromSource(
						org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
				.withDeprecation(null, null));
	}

	@Test
	public void innerClassRootConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassRootConfig.class);
		assertThat(metadata).has(Metadata.withProperty("config.name"));
	}

	@Test
	public void innerClassProperties() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassProperties.class);
		assertThat(metadata)
				.has(Metadata.withGroup("config").fromSource(InnerClassProperties.class));
		assertThat(metadata).has(
				Metadata.withGroup("config.first").ofType(InnerClassProperties.Foo.class)
						.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
		assertThat(metadata).has(
				Metadata.withGroup("config.the-second", InnerClassProperties.Foo.class)
						.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.name"));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.bar.name"));
		assertThat(metadata).has(Metadata.withGroup("config.third")
				.ofType(SimplePojo.class).fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.third.value"));
		assertThat(metadata).has(Metadata.withProperty("config.fourth"));
		assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
	}

	@Test
	public void innerClassAnnotatedGetterConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassAnnotatedGetterConfig.class);
		assertThat(metadata).has(Metadata.withProperty("specific.value"));
		assertThat(metadata).has(Metadata.withProperty("foo.name"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("specific.foo"));
	}

	@Test
	public void nestedClassChildProperties() throws Exception {
		ConfigurationMetadata metadata = compile(ClassWithNestedProperties.class);
		assertThat(metadata).has(Metadata.withGroup("nestedChildProps")
				.fromSource(ClassWithNestedProperties.NestedChildClass.class));
		assertThat(metadata)
				.has(Metadata
						.withProperty("nestedChildProps.child-class-property",
								Integer.class)
						.fromSource(ClassWithNestedProperties.NestedChildClass.class)
						.withDefaultValue(20));
		assertThat(metadata)
				.has(Metadata
						.withProperty("nestedChildProps.parent-class-property",
								Integer.class)
						.fromSource(ClassWithNestedProperties.NestedChildClass.class)
						.withDefaultValue(10));
	}

	@Test
	public void builderPojo() throws IOException {
		ConfigurationMetadata metadata = compile(BuilderPojo.class);
		assertThat(metadata).has(Metadata.withProperty("builder.name"));
	}

	@Test
	public void excludedTypesPojo() throws IOException {
		ConfigurationMetadata metadata = compile(ExcludedTypesPojo.class);
		assertThat(metadata).has(Metadata.withProperty("excluded.name"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.class-loader"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.data-source"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.print-writer"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer-array"));
	}

	@Test
	public void invalidAccessor() throws IOException {
		ConfigurationMetadata metadata = compile(InvalidAccessorProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config"));
		assertThat(metadata.getItems()).hasSize(1);
	}

	@Test
	public void doubleRegistration() throws IOException {
		ConfigurationMetadata metadata = compile(DoubleRegistrationProperties.class);
		assertThat(metadata).has(Metadata.withGroup("one"));
		assertThat(metadata).has(Metadata.withGroup("two"));
		assertThat(metadata).has(Metadata.withProperty("one.value"));
		assertThat(metadata).has(Metadata.withProperty("two.value"));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	public void invalidDoubleRegistration() throws IOException {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Compilation failed");
		compile(InvalidDoubleRegistrationProperties.class);
	}

	@Test
	public void genericTypes() throws IOException {
		ConfigurationMetadata metadata = compile(GenericConfig.class);
		assertThat(metadata).has(Metadata.withGroup("generic").ofType(
				"org.springframework.boot.configurationsample.specific.GenericConfig"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo").ofType(
				"org.springframework.boot.configurationsample.specific.GenericConfig$Foo"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar").ofType(
				"org.springframework.boot.configurationsample.specific.GenericConfig$Bar"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar.biz").ofType(
				"org.springframework.boot.configurationsample.specific.GenericConfig$Bar$Biz"));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.name")
				.ofType(String.class).fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-bar")
				.ofType("java.util.Map<java.lang.String,org.springframework.boot.configurationsample.specific.GenericConfig.Bar<java.lang.Integer>>")
				.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-integer")
				.ofType("java.util.Map<java.lang.String,java.lang.Integer>")
				.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.name")
				.ofType("java.lang.String").fromSource(GenericConfig.Bar.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.biz.name")
				.ofType("java.lang.String").fromSource(GenericConfig.Bar.Biz.class));
		assertThat(metadata.getItems()).hasSize(9);
	}

	@Test
	public void lombokDataProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokSimpleDataProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleDataProperties.class, "data");
	}

	@Test
	public void lombokSimpleProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokSimpleProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleProperties.class, "simple");
	}

	@Test
	public void lombokExplicitProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokExplicitProperties.class);
		assertSimpleLombokProperties(metadata, LombokExplicitProperties.class,
				"explicit");
	}

	@Test
	public void lombokInnerClassProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokInnerClassProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config")
				.fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withGroup("config.first")
				.ofType(LombokInnerClassProperties.Foo.class)
				.fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
		assertThat(metadata).has(
				Metadata.withGroup("config.second", LombokInnerClassProperties.Foo.class)
						.fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.second.name"));
		assertThat(metadata).has(Metadata.withProperty("config.second.bar.name"));
		assertThat(metadata)
				.has(Metadata.withGroup("config.third").ofType(SimpleLombokPojo.class)
						.fromSource(LombokInnerClassProperties.class));
		// For some reason the annotation processor resolves a type for SimpleLombokPojo
		// that is resolved (compiled) and the source annotations are gone. Because we
		// don't see the @Data annotation anymore, no field is harvested. What is crazy is
		// that a sample project works fine so this seem to be related to the unit test
		// environment for some reason. assertThat(metadata,
		// containsProperty("config.third.value"));
		assertThat(metadata).has(Metadata.withProperty("config.fourth"));
		assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
	}

	@Test
	public void lombokInnerClassWithGetterProperties() throws IOException {
		ConfigurationMetadata metadata = compile(
				LombokInnerClassWithGetterProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config")
				.fromSource(LombokInnerClassWithGetterProperties.class));
		assertThat(metadata).has(Metadata.withGroup("config.first")
				.ofType(LombokInnerClassWithGetterProperties.Foo.class)
				.fromSourceMethod("getFirst()")
				.fromSource(LombokInnerClassWithGetterProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	public void mergingOfAdditionalProperty() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty(null, "foo", "java.lang.String",
				AdditionalMetadata.class.getName(), null, null, null, null);
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).has(Metadata.withProperty("foo", String.class)
				.fromSource(AdditionalMetadata.class));
	}

	@Test
	public void mergeExistingPropertyDefaultValue() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "flag", null, null,
				null, null, true, null);
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class)
				.fromSource(SimpleProperties.class).withDescription("A simple flag.")
				.withDeprecation(null, null).withDefaultValue(true));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	public void mergeExistingPropertyDescription() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null,
				null, null, "A nice comparator.", null, null);
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata)
				.has(Metadata.withProperty("simple.comparator", "java.util.Comparator<?>")
						.fromSource(SimpleProperties.class)
						.withDescription("A nice comparator."));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	public void mergeExistingPropertyDeprecation() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null,
				null, null, null, null, new ItemDeprecation("Don't use this.",
						"simple.complex-comparator", "error"));
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata)
				.has(Metadata.withProperty("simple.comparator", "java.util.Comparator<?>")
						.fromSource(SimpleProperties.class).withDeprecation(
								"Don't use this.", "simple.complex-comparator", "error"));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	public void mergeExistingPropertyDeprecationOverride() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null,
				null, null, null, null,
				new ItemDeprecation("Don't use this.", "single.name"));
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(DeprecatedSingleProperty.class);
		assertThat(metadata).has(
				Metadata.withProperty("singledeprecated.name", String.class.getName())
						.fromSource(DeprecatedSingleProperty.class)
						.withDeprecation("Don't use this.", "single.name"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	public void mergeExistingPropertyDeprecationOverrideLevel() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null,
				null, null, null, null, new ItemDeprecation(null, null, "error"));
		writeAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(DeprecatedSingleProperty.class);
		assertThat(metadata).has(
				Metadata.withProperty("singledeprecated.name", String.class.getName())
						.fromSource(DeprecatedSingleProperty.class).withDeprecation(
								"renamed", "singledeprecated.new-name", "error"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	public void mergeOfInvalidAdditionalMetadata() throws IOException {
		File additionalMetadataFile = createAdditionalMetadataFile();
		FileCopyUtils.copy("Hello World", new FileWriter(additionalMetadataFile));

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Compilation failed");
		compile(SimpleProperties.class);
	}

	@Test
	public void mergingOfSimpleHint() throws Exception {
		writeAdditionalHints(ItemHint.newHint("simple.the-name",
				new ItemHint.ValueHint("boot", "Bla bla"),
				new ItemHint.ValueHint("spring", null)));
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class)
				.withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withHint("simple.the-name")
				.withValue(0, "boot", "Bla bla").withValue(1, "spring", null));
	}

	@Test
	public void mergingOfHintWithNonCanonicalName() throws Exception {
		writeAdditionalHints(ItemHint.newHint("simple.theName",
				new ItemHint.ValueHint("boot", "Bla bla")));
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class)
				.withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(
				Metadata.withHint("simple.the-name").withValue(0, "boot", "Bla bla"));
	}

	@Test
	public void mergingOfHintWithProvider() throws Exception {
		writeAdditionalHints(new ItemHint("simple.theName",
				Collections.<ItemHint.ValueHint>emptyList(),
				Arrays.asList(
						new ItemHint.ValueProvider("first",
								Collections.<String, Object>singletonMap("target",
										"org.foo")),
						new ItemHint.ValueProvider("second", null))));
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class)
				.withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withHint("simple.the-name")
				.withProvider("first", "target", "org.foo").withProvider("second"));
	}

	@Test
	public void mergingOfAdditionalDeprecation() throws Exception {
		writePropertyDeprecation(ItemMetadata.newProperty("simple", "wrongName",
				"java.lang.String", null, null, null, null,
				new ItemDeprecation("Lame name.", "simple.the-name")));
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.wrong-name", String.class)
				.withDeprecation("Lame name.", "simple.the-name"));
	}

	@Test
	public void mergingOfAdditionalMetadata() throws Exception {
		File metaInfFolder = new File(this.compiler.getOutputLocation(), "META-INF");
		metaInfFolder.mkdirs();
		File additionalMetadataFile = new File(metaInfFolder,
				"additional-spring-configuration-metadata.json");
		additionalMetadataFile.createNewFile();
		JSONObject property = new JSONObject();
		property.put("name", "foo");
		property.put("type", "java.lang.String");
		property.put("sourceType", AdditionalMetadata.class.getName());
		JSONArray properties = new JSONArray();
		properties.put(property);
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("properties", properties);
		FileWriter writer = new FileWriter(additionalMetadataFile);
		writer.append(additionalMetadata.toString(2));
		writer.flush();
		writer.close();
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).has(Metadata.withProperty("foo", String.class)
				.fromSource(AdditionalMetadata.class));
	}

	@Test
	public void incrementalBuild() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		assertThat(project.getOutputFile(MetadataStore.METADATA_PATH).exists()).isFalse();
		ConfigurationMetadata metadata = project.fullBuild();
		assertThat(project.getOutputFile(MetadataStore.METADATA_PATH).exists()).isTrue();
		assertThat(metadata).has(
				Metadata.withProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata).has(
				Metadata.withProperty("bar.counter").fromSource(BarProperties.class));
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata).has(
				Metadata.withProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata).has(
				Metadata.withProperty("bar.counter").fromSource(BarProperties.class));
		project.addSourceCode(BarProperties.class,
				BarProperties.class.getResourceAsStream("BarProperties.snippet"));
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata).has(Metadata.withProperty("bar.extra"));
		assertThat(metadata).has(Metadata.withProperty("foo.counter"));
		assertThat(metadata).has(Metadata.withProperty("bar.counter"));
		project.revert(BarProperties.class);
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("bar.extra"));
		assertThat(metadata).has(Metadata.withProperty("foo.counter"));
		assertThat(metadata).has(Metadata.withProperty("bar.counter"));
	}

	@Test
	public void incrementalBuildAnnotationRemoved() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		ConfigurationMetadata metadata = project.fullBuild();
		assertThat(metadata).has(Metadata.withProperty("foo.counter"));
		assertThat(metadata).has(Metadata.withProperty("bar.counter"));
		project.replaceText(BarProperties.class, "@ConfigurationProperties",
				"//@ConfigurationProperties");
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata).has(Metadata.withProperty("foo.counter"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("bar.counter"));
	}

	@Test
	public void incrementalBuildTypeRenamed() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		ConfigurationMetadata metadata = project.fullBuild();
		assertThat(metadata).has(
				Metadata.withProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata).has(
				Metadata.withProperty("bar.counter").fromSource(BarProperties.class));
		assertThat(metadata).doesNotHave(Metadata.withProperty("bar.counter")
				.fromSource(RenamedBarProperties.class));
		project.delete(BarProperties.class);
		project.add(RenamedBarProperties.class);
		metadata = project.incrementalBuild(RenamedBarProperties.class);
		assertThat(metadata).has(
				Metadata.withProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata).doesNotHave(
				Metadata.withProperty("bar.counter").fromSource(BarProperties.class));
		assertThat(metadata).has(Metadata.withProperty("bar.counter")
				.fromSource(RenamedBarProperties.class));
	}

	private void assertSimpleLombokProperties(ConfigurationMetadata metadata,
			Class<?> source, String prefix) {
		assertThat(metadata).has(Metadata.withGroup(prefix).fromSource(source));
		assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".id"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".name", String.class)
				.fromSource(source).withDescription("Name description."));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".description"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".counter"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".number")
				.fromSource(source).withDefaultValue(0).withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".items"));
		assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".ignored"));
	}

	private ConfigurationMetadata compile(Class<?>... types) throws IOException {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getMetadata();
	}

	private void writeAdditionalMetadata(ItemMetadata... metadata) throws Exception {
		TestJsonConverter converter = new TestJsonConverter();
		File additionalMetadataFile = createAdditionalMetadataFile();
		JSONObject additionalMetadata = new JSONObject();
		JSONArray properties = new JSONArray();
		for (ItemMetadata itemMetadata : metadata) {
			properties.put(converter.toJsonObject(itemMetadata));
		}
		additionalMetadata.put("properties", properties);
		writeMetadata(additionalMetadataFile, additionalMetadata);
	}

	private void writeAdditionalHints(ItemHint... hints) throws Exception {
		TestJsonConverter converter = new TestJsonConverter();
		File additionalMetadataFile = createAdditionalMetadataFile();
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("hints", converter.toJsonArray(Arrays.asList(hints)));
		writeMetadata(additionalMetadataFile, additionalMetadata);
	}

	private void writePropertyDeprecation(ItemMetadata... items) throws Exception {
		File additionalMetadataFile = createAdditionalMetadataFile();
		JSONArray propertiesArray = new JSONArray();
		for (ItemMetadata item : items) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", item.getName());
			if (item.getType() != null) {
				jsonObject.put("type", item.getType());
			}
			ItemDeprecation deprecation = item.getDeprecation();
			if (deprecation != null) {
				JSONObject deprecationJson = new JSONObject();
				if (deprecation.getReason() != null) {
					deprecationJson.put("reason", deprecation.getReason());
				}
				if (deprecation.getReplacement() != null) {
					deprecationJson.put("replacement", deprecation.getReplacement());
				}
				jsonObject.put("deprecation", deprecationJson);
			}
			propertiesArray.put(jsonObject);

		}
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("properties", propertiesArray);
		writeMetadata(additionalMetadataFile, additionalMetadata);
	}

	private File createAdditionalMetadataFile() throws IOException {
		File metaInfFolder = new File(this.compiler.getOutputLocation(), "META-INF");
		metaInfFolder.mkdirs();
		File additionalMetadataFile = new File(metaInfFolder,
				"additional-spring-configuration-metadata.json");
		additionalMetadataFile.createNewFile();
		return additionalMetadataFile;
	}

	private void writeMetadata(File metadataFile, JSONObject metadata) throws Exception {
		FileWriter writer = new FileWriter(metadataFile);
		try {
			writer.append(metadata.toString(2));
		}
		finally {
			writer.close();
		}
	}

	private static class AdditionalMetadata {

	}

}
