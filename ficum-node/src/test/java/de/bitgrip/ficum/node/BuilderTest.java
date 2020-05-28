package de.bitgrip.ficum.node;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class BuilderTest {

    @Test()
    public void testAndOperatorConstraint() {
        Node root = Builder.start().constraint("first", Comparison.EQUALS, 1L).and()
                .constraint("second", Comparison.NOT_EQUALS, 2L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(LogicalOperationNode.class));

        LogicalOperationNode node = (LogicalOperationNode) root;

        Assert.assertEquals(Operator.AND, node.getOperator());

        Node left = node.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = node.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));
        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;

        Assert.assertEquals("second", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.NOT_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(2l, rightConstraint.getArgument());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteComparisonConstraint() {
        Builder.start().constraint("first", null, 1L).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteSelectorConstraint() {
        Builder.start().constraint((String) null, Comparison.EQUALS, 1L).build();
    }

    @Test()
    public void testInfixToPostfix_NaturalOrderLeft() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.OR);
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_NaturalOrderRight() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededNested() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("fifth", Comparison.LESS_THAN, "5"));
        input.push(Operator.AND);
        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("fourth", Comparison.EQUALS, 4f));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.LEFT);
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(Operator.OR);
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("fifth", Comparison.LESS_THAN, "5"));
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("fourth", Comparison.EQUALS, 4f));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededOrderLeft() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.AND);
        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededOrderRight() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testIterableComparableConstraint() {
        Node root = Builder.start().constraint("first", Comparison.WITHIN, 1, 2.2, 3.3f, "x.x").build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Comparable<?>[] expected = {1, 2.2, 3.3f, "x.x"};
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableDoubleConstraint() {
        Node root = Builder.start().constraint("first", Comparison.WITHIN, 1.1, 2.2, 3.3).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Double[] expected = {1.1, 2.2, 3.3};
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableFewDoubleConstraint() {
        Double[] array = {1.1};
        Node root = Builder.start().constraint("first", Comparison.WITHIN, array).build();
        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Double expected = 1.1;
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableFewStringConstraint() {
        String[] array = {"hello"};
        Node root = Builder.start().constraint("first", Comparison.WITHIN, array).build();
        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        String expected = "hello";
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableIntegerConstraint() {
        Node root = Builder.start().constraint("first", Comparison.WITHIN, 1, 2, 3).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Integer[] expected = {1, 2, 3};
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableFewIntegerConstraint() {
        Integer[] array = {1};
        Node root = Builder.start().constraint("first", Comparison.WITHIN, array).build();
        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Integer expected = 1;
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testNaturalPrecededConstraint() {
        Node root = Builder.start().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).and()
                .constraint("third", Comparison.GREATER_EQUALS, 3L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(LogicalOperationNode.class));

        LogicalOperationNode orNode = (LogicalOperationNode) root;

        Assert.assertEquals(Operator.OR, orNode.getOperator());

        Node left = orNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = orNode.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(LogicalOperationNode.class));
        LogicalOperationNode andNode = (LogicalOperationNode) right;
        Assert.assertTrue(andNode.getLeft().getClass().isAssignableFrom(ConstraintNode.class));
        Assert.assertTrue(andNode.getRight().getClass().isAssignableFrom(ConstraintNode.class));
    }

    @Test()
    public void testOrOperatorConstraint() {
        Node root = Builder.start().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(LogicalOperationNode.class));

        LogicalOperationNode orNode = (LogicalOperationNode) root;

        Assert.assertEquals(Operator.OR, orNode.getOperator());

        Node left = orNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = orNode.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));
        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;

        Assert.assertEquals("second", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.NOT_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(2l, rightConstraint.getArgument());
    }

    @Test()
    public void testSimpleConstraint() {
        Node n = Builder.start().constraint("first", Comparison.EQUALS, 1L).build();
        Assert.assertTrue(n.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> constraint = (ConstraintNode<?>) n;

        Assert.assertEquals("first", constraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, constraint.getComparison());
        Assert.assertEquals(1l, constraint.getArgument());
    }

    @Test()
    public void testSelectorConstraint() {
        Node n = Builder.start().constraint(new SimpleSelector("first"), Comparison.EQUALS, 1L).build();
        Assert.assertTrue(n.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> constraint = (ConstraintNode<?>) n;

        Assert.assertEquals("first", constraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, constraint.getComparison());
        Assert.assertEquals(1l, constraint.getArgument());
    }

    @Test(expected = IllegalStateException.class)
    public void testSubCloseConstraint() {
        Builder.start().constraint("first", Comparison.EQUALS, 1L).endsub().build();
    }

    @Test(expected = IllegalStateException.class)
    public void testSubOpenConstraint() {
        Builder.start().constraint("first", Comparison.EQUALS, 1L).or().sub()
                .constraint("second", Comparison.NOT_EQUALS, 2L).build();
    }

    @Test()
    public void testSubPrecededConstraint() {
        Node root = Builder.start().sub().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).endsub().and()
                .constraint("third", Comparison.GREATER_EQUALS, 3L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(LogicalOperationNode.class));

        LogicalOperationNode andNode = (LogicalOperationNode) root;

        Assert.assertEquals(Operator.AND, andNode.getOperator());

        Node right = andNode.getRight();
        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;
        Assert.assertEquals("third", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.GREATER_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(3l, rightConstraint.getArgument());

        Node left = andNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(LogicalOperationNode.class));

        LogicalOperationNode orNode = (LogicalOperationNode) left;
        Assert.assertTrue(orNode.getLeft().getClass().isAssignableFrom(ConstraintNode.class));
        Assert.assertTrue(orNode.getRight().getClass().isAssignableFrom(ConstraintNode.class));
    }

}
