package de.bitgrip.ficum.visitor;

import de.bitgrip.ficum.node.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class JPAPredicateVisitor<T> extends AbstractVisitor<Predicate> {

    public static final char ESCAPE_CHAR = '\\';

    private CriteriaBuilder criteriaBuilder;

    private Root<T> root;

    private Class<T> queryClass;

    private List<Predicate> predicates;

    private Set<Class<? extends Comparable<?>>> mappedTypes = new HashSet<Class<? extends Comparable<?>>>();

    public JPAPredicateVisitor(Class<T> queryClass, Root<T> root, CriteriaBuilder criteriaBuilder) {
        super();
        this.queryClass = queryClass;
        this.criteriaBuilder = criteriaBuilder;
        this.root = root;
        mappedTypes.add(String.class);
        mappedTypes.add(Character.class);
        mappedTypes.add(Boolean.class);
        mappedTypes.add(Double.class);
        mappedTypes.add(Integer.class);
        mappedTypes.add(Float.class);
        mappedTypes.add(Short.class);
        mappedTypes.add(Long.class);
        mappedTypes.add(Byte.class);
        mappedTypes.add(BigInteger.class);
        mappedTypes.add(BigDecimal.class);
        mappedTypes.add(Date.class);
        mappedTypes.add(Calendar.class);
        mappedTypes.add(UUID.class);
        mappedTypes.add(OffsetDateTime.class);
        mappedTypes.add(LocalDate.class);
    }

    private static boolean containsEscapedChar(String value) {
        return value.contains("\\%") || value.contains("\\\\") || value.contains("\\_");
    }

    public boolean addMappedType(Class<? extends Comparable<?>> mappedType) {
        return mappedTypes.add(mappedType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildEquals(Object value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (value == null) {
            pred = path.isNull();
        } else if (path.getJavaType().equals(String.class)) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = Wildcards.escapeAndConvertToSQLWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = criteriaBuilder.like((Expression<String>) path, theValue, ESCAPE_CHAR);
                } else {
                    pred = criteriaBuilder.like((Expression<String>) path, theValue);
                }
            } else {
                pred = criteriaBuilder.equal(path, originalValue);
            }
        } else {
            pred = criteriaBuilder.equal(path, value);
        }
        return pred;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildNotEquals(Object value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (value == null) {
            pred = path.isNotNull();
        } else if (path.getJavaType().equals(String.class)) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = Wildcards.escapeAndConvertToSQLWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = criteriaBuilder.notLike((Expression<String>) path, theValue, ESCAPE_CHAR);
                } else {
                    pred = criteriaBuilder.notLike((Expression<String>) path, theValue);
                }
            } else {
                pred = criteriaBuilder.notEqual(path, originalValue);
            }
        } else {
            pred = criteriaBuilder.notEqual(path, value);
        }
        return pred;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate doBuildCollectionSizePredicate(Comparison comparison, Path<?> path, Integer argument) {
        Expression<Integer> exp = criteriaBuilder.size((Expression<? extends Collection>) path);

        switch (comparison) {
            case GREATER_THAN:
                return criteriaBuilder.greaterThan(exp, argument);

            case EQUALS:
                return criteriaBuilder.equal(exp, argument);

            case NOT_EQUALS:
                return criteriaBuilder.notEqual(exp, argument);

            case LESS_THAN:
                return criteriaBuilder.lessThan(exp, argument);

            case LESS_EQUALS:
                return criteriaBuilder.lessThanOrEqualTo(exp, argument);

            case GREATER_EQUALS:
                return criteriaBuilder.greaterThanOrEqualTo(exp, argument);

            default:
                return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate doBuildPredicate(Comparison comparison, Expression<? extends Comparable> path,
                                       Comparable argument) {

        switch (comparison) {
            case GREATER_THAN:
                return criteriaBuilder.greaterThan(path, argument);

            case EQUALS:
                return buildEquals(argument, path);

            case NOT_EQUALS:
                return buildNotEquals(argument, path);

            case LESS_THAN:
                return criteriaBuilder.lessThan(path, argument);

            case LESS_EQUALS:
                return criteriaBuilder.lessThanOrEqualTo(path, argument);

            case GREATER_EQUALS:
                return criteriaBuilder.greaterThanOrEqualTo(path, argument);

            default:
                return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate doBuildPredicate(Comparison comparison, Expression<? extends Comparable> path,
                                       List<Comparable> argument) {

        switch (comparison) {
            case IN:
                return path.in(argument);

            case NIN:
                return criteriaBuilder.not(path.in(argument));

            default:
                return null;
        }
    }

    private Path<?> findPath(String names) {
        Path<?> path = root;
        Class<?> clazz = queryClass;

        Iterator<String> namesIterator = Arrays.asList(names.split("\\.")).iterator();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            boolean isLast = !namesIterator.hasNext();

            Field field = getField(clazz, name);
            clazz = field.getType();

            if (isMappedType(clazz)) {
                if (isLast)
                    path = path.get(name);
                else
                    throw new IllegalArgumentException(String
                            .format("%s resolves to %s and can not contain a nested property", name, clazz.getName()));
            } else if (isCollection(clazz)) {
                clazz = getGenericTypeClazz((ParameterizedType) field.getGenericType());
                if (isLast && !isMappedType(clazz))
                    path = path.get(name);
                else {
                    path = getOrCreateJoin(path, name);
                }
            } else if (path instanceof From) {
                path = getOrCreateJoin(path, name);
            }
        }

        if (path == null)
            throw new IllegalArgumentException(
                    String.format("%s can not be applied to %s", names, queryClass.getName()));

        return path;
    }

    private Path<?> getExistingJoin(From<?, ?> element, String prop) {
        final Set<?> joins = element.getJoins();
        for (Object object : joins) {
            Join<?, ?> join = (Join<?, ?>) object;
            if (join.getAttribute().getName().equals(prop)) {
                return join;
            }
        }
        return null;
    }

    private Field getField(Class<?> clazz, String name) {
        Field field = FieldUtils.getField(clazz, name, true);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Can not find field %s in %s", name, clazz.getName()));
        }
        return field;
    }

    private Class<?> getGenericTypeClazz(ParameterizedType type) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(type);
        Iterator<Type> it = typeArguments.values().iterator();
        if (it.hasNext()) {
            return TypeUtils.getRawType(it.next(), null);
        }
        return Object.class;
    }

    private Path<?> getOrCreateJoin(Path<?> path, String name) {
        Path<?> ret = getExistingJoin((From<?, ?>) path, name);
        if (ret != null) {
            return ret;
        } else {
            return path.equals(root) ? root.join(name) : ((From<?, ?>) path).join(name);
        }
    }

    private boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    private boolean isCollectionSizeCheck(Path<?> path, Comparable<?> argument) {
        return isCollection(path.getJavaType()) && argument instanceof Integer;
    }

    private boolean isMappedType(Class<?> clazz) {
        for (Class<? extends Comparable<?>> mappedType : mappedTypes) {
            if (mappedType.isAssignableFrom(clazz) || clazz.isEnum()) {
                return true;
            }
        }
        return false;
    }

    private Comparable<?> convertValue(Comparable<?> value,  Class<? extends Comparable> clazz ){
        // convert from string to enum
        if (value instanceof String && clazz.isEnum()) {
            value = Enum.valueOf((Class<? extends Enum>) clazz, value.toString());
        }

        // convert date and time types
        if (value instanceof LocalDate && clazz.isAssignableFrom(Date.class)) {
            value = Date.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC"))
                    .toInstant());
        }

        if (value instanceof LocalDate && clazz.isAssignableFrom(Calendar.class)) {
            value = GregorianCalendar.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC")));
        }

        if (value instanceof LocalDate && clazz.isAssignableFrom(OffsetDateTime.class)) {
            value = OffsetDateTime.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC")));
        }

        if (value instanceof OffsetDateTime && clazz.isAssignableFrom(Date.class)) {
            value = Date.from(((OffsetDateTime) value).toInstant());
        }

        if (value instanceof OffsetDateTime && clazz.isAssignableFrom(Calendar.class)) {
            value = GregorianCalendar.from(((OffsetDateTime) value).toZonedDateTime());
        }
        return value;
    }

    public Predicate start(Node node) {
        predicates = new ArrayList<>();
        node.accept(this);
        return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void visit(ConstraintNode node) {
        Path<?> path = findPath(getMappedField(node.getSelector()));

        Class<? extends Comparable> clazz = (Class<? extends Comparable>) path.getJavaType();
        Object argument = node.getArgument();

        Predicate pred = null;
        if (argument instanceof Comparable<?>) {
            Comparable<?> value = (Comparable<?>) argument;

            value = convertValue(value,clazz);

            pred = isCollectionSizeCheck(path, value)
                    ? doBuildCollectionSizePredicate(node.getComparison(), path, (Integer) value)
                    : doBuildPredicate(node.getComparison(), path.as(clazz), value);

        } else if (argument instanceof List) {
            //convert all values to the supported data-type
            List<Comparable<?>> transformedValues = ((List<Comparable<?>>) argument).stream().map(v -> convertValue(v, clazz)).collect(Collectors.toList());
            pred = doBuildPredicate(node.getComparison(), path.as(clazz), sanatizeToComparable(transformedValues));
        } else if (argument == null) {
            pred = doBuildPredicate(node.getComparison(), path.as(clazz), (Comparable<?>) null);
        } else {
            throw new IllegalArgumentException("Unable to handle argument of type " + argument.getClass().getName());
        }

        if (pred != null) {
            predicates.add(pred);
        } else {
            throw new IllegalArgumentException("Constraint: " + node + " does not resolve to a predicate");
        }

    }

    public void visit(OperationNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);

        Predicate pred = null;
        switch (node.getOperator()) {
            case AND:
                pred = criteriaBuilder.and(predicates.get(0), predicates.get(1));
                break;

            case OR:
                pred = criteriaBuilder.or(predicates.get(0), predicates.get(1));
                break;

            case NAND:
                pred = criteriaBuilder.or(criteriaBuilder.not(predicates.get(0)), criteriaBuilder.not(predicates.get(1)));
                break;

            case NOR:
                pred = criteriaBuilder.and(criteriaBuilder.not(predicates.get(0)), criteriaBuilder.not(predicates.get(1)));
                break;

            default:
                throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

        predicates.clear();
        predicates.add(pred);
    }

}
