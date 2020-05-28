package de.bitgrip.ficum.node;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * A Builder to help building an {@link Node} tree from an infix stack.
 */
public class Builder {

    private Deque<Object> infixStack;

    private UnbalancedBuilder unbalancedBuilder;

    private DefinedBuilder definedBuilder;

    private Builder parent;

    private Builder() {
        infixStack = new ArrayDeque<Object>();
        unbalancedBuilder = new UnbalancedBuilder(this);
        definedBuilder = new DefinedBuilder();
    }

    private Builder(Builder parent) {
        this();
        this.parent = parent;
    }

    /**
     * Build a {@link Node} tree from an infix stack
     *
     * @param stack
     * @return {@link Node} root node of the tree
     */
    public static Node build(Iterable<Object> stack) {
        if (stack == null) {
            return null;
        }
        return eval(infixToPostfix(stack));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Node eval(Deque<Object> postfix) {
        while (!postfix.isEmpty()) {
            Object element = postfix.removeFirst();
            if (element instanceof Constraint) {
                Constraint<?> constraint = (Constraint<?>) element;
                return new ConstraintNode(constraint);
            }

            if (element instanceof Operator) {
                Operator operator = (Operator) element;
                OperationNode node = new LogicalOperationNode(operator);
                Node operand1 = eval(postfix);
                Node operand2 = eval(postfix);
                node.setRight(operand1);
                node.setLeft(operand2);
                return node;
            }
        }
        return null;
    }

    protected static Deque<Object> infixToPostfix(Iterable<Object> infixStack) {
        Deque<Object> output = new ArrayDeque<Object>();
        Deque<Operator> operatorStack = new ArrayDeque<Operator>();

        for (Object element : infixStack) {
            if (element instanceof Constraint) {
                output.push(element);
            }

            if (element instanceof Operator) {
                Operator op = (Operator) element;

                switch (op) {
                case AND:
                case NOR:
                case LEFT:
                    operatorStack.push(op);
                    break;

                case OR:
                case NAND:
                    while (!operatorStack.isEmpty() && operatorStack.peek().preceded) {
                        output.push(operatorStack.pop());
                    }
                    operatorStack.push(op);
                    break;

                case RIGHT:
                    boolean balanced = false;
                    while (!operatorStack.isEmpty()) {
                        Operator last = operatorStack.pop();
                        if (Operator.LEFT.equals(last)) {
                            balanced = true;
                            break;
                        }
                        output.push(last);
                    }
                    if (!balanced) {
                        throw new IllegalStateException(
                                "Unbalanced subexpression! Make sure subexpressions are closed properly.");
                    }
                    break;

                default:
                    break;
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            output.push(operatorStack.pop());
        }

        return output;
    }

    protected static Deque<Object> reverse(Iterable<Object> stack) {
        Deque<Object> deque = new ArrayDeque<Object>();
        for (Object element : stack) {
            deque.addFirst(element);
        }
        return deque;
    }

    /**
     * Creates a new instance of the builder.
     *
     * @return new builder instance
     */
    public static UnbalancedBuilder start() {
        return new Builder().unbalancedBuilder;
    }

    public class DefinedBuilder {

        /**
         * Add an AND {@link Operator} to the stack
         *
         * @return {@link Builder} this builder object
         */
        public UnbalancedBuilder and() {
            infixStack.push(Operator.AND);
            return unbalancedBuilder;
        }

        /**
         * Build a {@link Node} tree
         *
         * @return {@link Node} root node of the tree
         */
        public Node build() {
            if (parent != null) {
                throw new IllegalStateException("Can not build! Close subexpression first.");
            }
            return eval(infixToPostfix(reverse(infixStack)));
        }

        /**
         * Add an RIGHT {@link Operator} to the stack. Think of it as a right
         * parenthesis.
         *
         * @return {@link Builder} this builder object
         */
        public DefinedBuilder endsub() {
            if (parent == null) {
                throw new IllegalStateException("No open subexpression found!");
            }
            parent.infixStack.push(Operator.LEFT);
            while (!infixStack.isEmpty()) {
                parent.infixStack.push(infixStack.removeLast());
            }
            parent.infixStack.push(Operator.RIGHT);
            return parent.definedBuilder;
        }

        /**
         * Add an OR {@link Operator} to the stack
         *
         * @return {@link Builder} this builder object
         */
        public UnbalancedBuilder or() {
            infixStack.push(Operator.OR);
            return unbalancedBuilder;
        }

    }

    public class UnbalancedBuilder {
        private Builder builder;

        public UnbalancedBuilder(Builder builder) {
            this.builder = builder;
        }

        /**
         * Add a {@link Constraint} to the stack
         *
         * @param selector
         *            identifier for an field this constraint applies to
         * @param comparison
         *            {@link Comparison} to apply
         * @param argument
         *            {@link Comparable} to process against the value of the
         *            selected field
         * @return {@link Builder} this builder object
         */
        public DefinedBuilder constraint(String selector, Comparison comparison, Comparable<?> argument) {
            infixStack.push(new Constraint<Comparable<?>>(selector, comparison, argument));
            return definedBuilder;
        }

        /**
         * Add a {@link Constraint} to the stack
         *
         * @param selector
         *            identifier for an field this constraint applies to
         * @param comparison
         *            {@link Comparison} to apply
         * @param argument
         *            {@link Comparable} to process against the value of the
         *            selected field
         * @return {@link Builder} this builder object
         */
        public DefinedBuilder constraint(Selector selector, Comparison comparison, Comparable<?> argument) {
            infixStack.push(new Constraint<Comparable<?>>(selector, comparison, argument));
            return definedBuilder;
        }

        /**
         * Add a {@link Constraint} to the stack
         *
         * @param selector
         *            identifier for an field this constraint applies to
         * @param comparison
         *            {@link Comparison} to apply
         * @param argument
         *            Array of Comparable to process against the value of the
         *            selected field
         * @return {@link Builder} this builder object
         */
        public DefinedBuilder constraint(String selector, Comparison comparison, Comparable<?>... argument) {
            if (argument.length == 0) {
                throw new IllegalArgumentException("Comparable argument array should have at least 2 members.");
            }
            if (argument.length == 1) {
                infixStack.push(new Constraint<Comparable<?>>(selector, comparison, argument[0]));
            }
            infixStack.push(new Constraint<Iterable<Comparable<?>>>(selector, comparison, Arrays.asList(argument)));
            return definedBuilder;
        }

        /**
         * Add an LEFT {@link Operator} to the stack. Think of it as a left
         * parenthesis.
         *
         * @return {@link Builder} this builder object
         */
        public UnbalancedBuilder sub() {
            return new Builder(builder).unbalancedBuilder;
        }

    }

}
