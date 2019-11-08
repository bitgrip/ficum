package de.bitgrip.ficum.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>FicumExpression which marks, that the parameter is a FicumExpression with defined allowedSectorNames.</p>
 * <p>
 * (w) marcelmuller<br/>
 * (c) bitgrip GmbH, 2019
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface FicumExpression {

  /**
   * List of allowedSectorNames
   * @return
   */
  String[] value();

}
