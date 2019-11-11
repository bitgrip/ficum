package de.bitgrip.ficum.converter.spring;

import de.bitgrip.ficum.annotation.FicumExpression;
import de.bitgrip.ficum.node.Builder;
import de.bitgrip.ficum.node.Comparison;
import de.bitgrip.ficum.node.Node;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

/**
 * <p>Tests for GenericFicumConverter</p>
 * <p>
 * (w) marcelmuller<br/>
 * (c) bitgrip GmbH, 2019
 * </p>
 */
public class FicumNodeConverterTest {

  private FicumNodeConverter ficumNodeConverter = new FicumNodeConverter();

  @Test
  public void convert_string_to_node_with_acceptable_source() throws NoSuchFieldException {
    String expression = "foo=='abc'";
    TypeDescriptor sourceType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("stringField"));
    TypeDescriptor targetType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("nodeFieldWithAnnotation"));

    Node node = (Node) ficumNodeConverter.convert(expression, sourceType, targetType);

    Assert.assertNotNull(node);
  }

  @Test(expected = IllegalStateException.class)
  public void convert_string_to_node__with_not_annotated_field() throws NoSuchFieldException {
    String expression = "foo=='abc'";
    TypeDescriptor sourceType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("stringField"));
    TypeDescriptor targetType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("nodeFieldWithoutAnnotation"));

    ficumNodeConverter.convert(expression, sourceType, targetType);
  }

  @Test(expected = IllegalArgumentException.class)
  public void convert_string_to_node__with_not_acceptable_source() throws NoSuchFieldException {
    String expression = "foo=='abc'";
    TypeDescriptor sourceType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("sourceObjectField"));
    TypeDescriptor targetType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("nodeFieldWithAnnotation"));

    ficumNodeConverter.convert(expression, sourceType, targetType);
  }

  @Test
  public void convert_node_to_string_with_acceptable_source() throws NoSuchFieldException {
    Node filter = Builder.start().constraint("foo", Comparison.EQUALS, "ba").build();
    TypeDescriptor sourceType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("nodeFieldWithAnnotation"));
    TypeDescriptor targetType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("stringField"));

    String result = (String) ficumNodeConverter.convert(filter, sourceType,targetType);

    Assert.assertEquals("foo=='ba'", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void convert_node_to_string_with_not_acceptable_source() throws NoSuchFieldException {
    Node filter = Builder.start().constraint("foo", Comparison.EQUALS, "ba").build();
    TypeDescriptor sourceType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("sourceObjectField"));
    TypeDescriptor targetType = new TypeDescriptor(DummyFicumTestClass.class.getDeclaredField("stringField"));

    String result = (String) ficumNodeConverter.convert(filter, sourceType,targetType);

  }

  public class DummyFicumTestClass{

    @FicumExpression("foo")
    private Node nodeFieldWithAnnotation;

    private Node nodeFieldWithoutAnnotation;

    private String stringField;

    private Object sourceObjectField;

  }
}
