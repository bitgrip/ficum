package de.bitgrip.ficum.converter.spring;

import de.bitgrip.ficum.annotation.FicumExpression;
import de.bitgrip.ficum.node.Node;
import de.bitgrip.ficum.node.QueryPrinterVisitor;
import de.bitgrip.ficum.parser.ParseHelper;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Generic Converter which is able to Convert a String to a Ficum Node</p>
 * <p>
 * (w) marcelmuller<br/>
 * (c) bitgrip GmbH, 2019
 * </p>
 */
public class FicumNodeConverter implements GenericConverter {

  @Override
  public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
    Set<GenericConverter.ConvertiblePair> pairs = new HashSet<>();
    pairs.add(new GenericConverter.ConvertiblePair(String.class, Node.class));
    pairs.add(new GenericConverter.ConvertiblePair(Node.class, String.class));
    return Collections.unmodifiableSet(pairs);
  }

  @Override
  public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (sourceType.getType().equals(String.class)) {
      FicumExpression ficumExpressionAnnotation = targetType.getAnnotation(FicumExpression.class);
      if (ficumExpressionAnnotation == null || ficumExpressionAnnotation.value().length == 0) {
        throw new IllegalStateException("missing " + FicumExpression.class.getName() + " annotation to node parameter" +
                " for defining allowedSectorNames");
      }
      return convertStringToNode((String) source, ficumExpressionAnnotation.value());
    } else if (Node.class.isAssignableFrom(sourceType.getType())) {
      return convertNodeToString((Node) source);
    }

    throw new IllegalArgumentException("Invalid source type " + source.getClass().getCanonicalName());
  }

  private Node convertStringToNode(String ficumQuery, String[] allowedSectorNames) {
    return ParseHelper.parse(ficumQuery, allowedSectorNames);
  }

  private String convertNodeToString(Node filter) {
    QueryPrinterVisitor queryPrinterVisitor = new QueryPrinterVisitor();
    return queryPrinterVisitor.start(filter);
  }
}
