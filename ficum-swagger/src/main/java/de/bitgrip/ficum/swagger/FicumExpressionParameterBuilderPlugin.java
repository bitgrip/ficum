package de.bitgrip.ficum.swagger;

import com.fasterxml.classmate.TypeResolver;
import de.bitgrip.ficum.annotation.FicumExpression;
import org.springframework.core.annotation.Order;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.util.Optional;

/**
 * <p>Plugin which attaches a description to all parameters which are annotated with FicumExpression.
 * Additionally it adds all allowedValues which are configured by FicumExpression to formatDescription
 * </p>
 * <p>
 * (w) marcelmuller<br/>
 * (c) bitgrip GmbH, 2019
 * </p>
 */
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1000)
public class FicumExpressionParameterBuilderPlugin implements ParameterBuilderPlugin {

  private static final String DEFAULT_FORMAT_DESCRIPTION = "Filter string for filtering object using " +
          "ficum-query-language, checkout https://github.com/bitgrip/ficum#ficum-query-language. Allowed fieldNames " +
          "are: %s";

  private TypeResolver resolver;

  private String formatDescription;

  public FicumExpressionParameterBuilderPlugin(TypeResolver resolver, String formatDescription) {
    this.resolver = resolver;
    this.formatDescription = formatDescription;
  }

  public FicumExpressionParameterBuilderPlugin(TypeResolver resolver) {
    this(resolver, DEFAULT_FORMAT_DESCRIPTION);
  }

  @Override
  public void apply(ParameterContext parameterContext) {
    ResolvedMethodParameter methodParameter = parameterContext.resolvedMethodParameter();
    Optional<FicumExpression> ficumExpressionAnnotation = methodParameter.findAnnotation(FicumExpression.class).toJavaUtil();

    if (ficumExpressionAnnotation.isPresent()) {
      String joinedAllowedValues = String.join(", ", ficumExpressionAnnotation.get().value());
      parameterContext.parameterBuilder()
              .type(resolver.resolve(String.class))
              .description(String.format(formatDescription, joinedAllowedValues));
    }
  }

  @Override
  public boolean supports(DocumentationType delimiter) {
    return true;
  }
}
