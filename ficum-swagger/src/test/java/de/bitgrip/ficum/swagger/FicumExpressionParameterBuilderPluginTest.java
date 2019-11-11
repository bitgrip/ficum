package de.bitgrip.ficum.swagger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import de.bitgrip.ficum.annotation.FicumExpression;
import de.bitgrip.ficum.node.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.ParameterContext;

/**
 * <p>Test for FicumExpressionParameterBuilder</p>
 * <p>
 * (w) marcelmuller<br/>
 * (c) bitgrip GmbH, 2019
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class FicumExpressionParameterBuilderPluginTest {

  @Mock
  private ParameterContext parameterContext;

  @Test
  public void test_apply_with_default_description() throws NoSuchMethodException {
    TypeResolver typeResolver = new TypeResolver();
    FicumExpressionParameterBuilderPlugin ficumExpressionParameterBuilderPlugin =
            new FicumExpressionParameterBuilderPlugin(typeResolver);
    MethodParameter methodParameter = new MethodParameter(TestClass.class.getMethod("doSomethingWithFicumExpression", Node.class),0);

    final ResolvedType testClassType =
            typeResolver.resolve(
                    TestClass.class);
    ResolvedMethodParameter resolvedMethodParameter = new ResolvedMethodParameter("filter",methodParameter, testClassType);

    Mockito.when(parameterContext.resolvedMethodParameter()).thenReturn(resolvedMethodParameter);
    Mockito.when(parameterContext.parameterBuilder()).thenReturn(new ParameterBuilder());

    ficumExpressionParameterBuilderPlugin.apply(parameterContext);

    Parameter parameter = parameterContext.parameterBuilder().build();
    Assert.assertTrue(parameter.getDescription().contains("abc"));
    Assert.assertTrue(parameter.getDescription().contains("Filter string for filtering object using"));
    Assert.assertTrue(parameter.getType().isPresent());
    Assert.assertTrue(parameter.getType().get().getErasedType().equals(String.class));
  }

  @Test
  public void test_apply_with_default_description_no_annotation() throws NoSuchMethodException {
    TypeResolver typeResolver = new TypeResolver();
    FicumExpressionParameterBuilderPlugin ficumExpressionParameterBuilderPlugin =
            new FicumExpressionParameterBuilderPlugin(typeResolver);
    MethodParameter methodParameter = new MethodParameter(TestClass.class.getMethod("doSomethingWithoutFicumExpression", Node.class),0);

    final ResolvedType testClassType =
            typeResolver.resolve(
                    TestClass.class);
    ResolvedMethodParameter resolvedMethodParameter = new ResolvedMethodParameter("filter",methodParameter, testClassType);

    Mockito.when(parameterContext.resolvedMethodParameter()).thenReturn(resolvedMethodParameter);
    Mockito.when(parameterContext.parameterBuilder()).thenReturn(new ParameterBuilder());

    ficumExpressionParameterBuilderPlugin.apply(parameterContext);

    Parameter parameter = parameterContext.parameterBuilder().build();
    Assert.assertNull(parameter.getDescription());
    Assert.assertFalse(parameter.getType().isPresent());
  }

  @Test
  public void test_supports_return_true() {
    FicumExpressionParameterBuilderPlugin ficumExpressionParameterBuilderPlugin =
            new FicumExpressionParameterBuilderPlugin(null);
    Assert.assertTrue(ficumExpressionParameterBuilderPlugin.supports(DocumentationType.SPRING_WEB));
    Assert.assertTrue(ficumExpressionParameterBuilderPlugin.supports(DocumentationType.SWAGGER_2));
    Assert.assertTrue(ficumExpressionParameterBuilderPlugin.supports(DocumentationType.SWAGGER_12));
  }

  public class TestClass {

    public void doSomethingWithFicumExpression(@FicumExpression("abc") Node filter) {
      return;
    }

    public void doSomethingWithoutFicumExpression(Node filter) {
      return;
    }
  }
}
