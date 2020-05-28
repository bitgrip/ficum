package de.bitgrip.ficum.parser;

import de.bitgrip.ficum.node.Comparison;
import de.bitgrip.ficum.node.Constraint;
import de.bitgrip.ficum.node.Selector;
import de.bitgrip.ficum.node.SimpleSelector;
import org.parboiled.Action;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;

import java.util.*;

@BuildParseTree
public class ConstraintParser extends ArgumentParser {

    protected String[] allowedSelectors = {};

    public ConstraintParser(String... allowedSelectors) {
        super();
        this.allowedSelectors = allowedSelectors;
        Arrays.sort(this.allowedSelectors, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);
            }
        });
    }

    @Override
    public Rule root() {
        return Sequence(Constraint(), EOI);
    }

    @SuppressSubnodes
    protected Rule Selector() {
        return Sequence(FirstOf(allowedSelectors), new Action<Object>() {
            public boolean run(Context<Object> context) {
                return push(new SimpleSelector(match()));
            }
        });
    }

    protected Rule Comparison() {
        return Sequence(FirstOf(Comparison.allSigns()), new Action<Comparison>() {
            public boolean run(Context<Comparison> context) {
                return push(Comparison.from(match()));
            }
        });
    }

    protected Rule Constraint() {
        return Sequence(Selector(), Comparison(),
                FirstOf(Argument(), Sequence(Ch('['), Argument(),
                        ZeroOrMore(Sequence(Ch(','), Optional(Ch(' ')), Argument())), Ch(']'))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        List<Comparable<?>> arguments = new ArrayList<Comparable<?>>();
                        Comparison comparison = null;
                        Selector selector = null;
                        while (!context.getValueStack().isEmpty()) {
                            if (peek() instanceof Comparison) {
                                comparison = (Comparison) pop();
                            } else if (peek() instanceof Selector) {
                                selector = (Selector) pop();
                            } else if (isBaseType(context.getValueStack().peek())) {
                                arguments.add(context.getValueStack().pop());
                            } else {
                                break;
                            }
                        }

                        if (arguments.size() == 1) {
                            return push(new Constraint<Comparable<?>>(selector, comparison, arguments.get(0)));
                        } else {
                            Collections.reverse(arguments);
                            return push(new Constraint<List<Comparable<?>>>(selector, comparison, arguments));
                        }
                    }
                });
    }

}
