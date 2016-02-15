package com.tsystems.ficum.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.parboiled.Action;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;

import com.tsystems.ficum.node.Comparison;
import com.tsystems.ficum.node.Constraint;

@BuildParseTree
public class ConstraintParser extends ArgumentParser {

    protected String selector;

    protected Comparison comparison;

    protected String[] allowedSelectorNames = {};

    public ConstraintParser(String... allowedSelectorNames) {
        super();
        this.allowedSelectorNames = allowedSelectorNames;
        Arrays.sort(this.allowedSelectorNames, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);
            }
        });
    }

    protected Rule Comparison() {
        comparison = null;
        return Sequence(FirstOf(Comparison.allSigns()), new Action<Object>() {
            public boolean run(Context<Object> context) {
                comparison = Comparison.from(match());
                return true;
            }
        });
    }

    protected Rule Constraint() {
        return Sequence(Selector(), Comparison(),
                FirstOf(Argument(), Sequence(Ch('['), Argument(),
                        OneOrMore(Sequence(Ch(','), Optional(Ch(' ')), Argument())), Ch(']'))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        List<Comparable<?>> arguments = new ArrayList<Comparable<?>>();
                        while (!context.getValueStack().isEmpty() && isBaseType(context.getValueStack().peek())) {
                            arguments.add(context.getValueStack().pop());
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

    @Override
    public Rule root() {
        return Sequence(Constraint(), EOI);
    }

    @SuppressSubnodes
    protected Rule Selector() {
        selector = null;
        return Sequence(Sequence(FirstOf(allowedSelectorNames), ZeroOrMore(Ch('.'), FirstOf(allowedSelectorNames))),
                new Action<Object>() {
                    public boolean run(Context<Object> context) {
                        selector = match();
                        return true;
                    }
                });
    }

}