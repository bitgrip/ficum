package de.bitgrip.ficum.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.common.StringBuilderSink;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bitgrip.ficum.node.Comparison;
import de.bitgrip.ficum.node.Constraint;

public class ConstraintParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConstraintParserTest.class);

    private static final String[] allowedPaths = { "first", "second", "first.second", "first.second.third" };
    private static final ConstraintParser parser = Parboiled.createParser(ConstraintParser.class,
            (Object) allowedPaths);

    private TracingParseRunner<Constraint<?>> parseRunner;

    private StringBuilderSink sink;

    private void assertError(Class<?> expected, String input) {
        ParsingResult<Constraint<?>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        Assert.assertEquals(1, result.parseErrors.size());
        Assert.assertTrue(result.parseErrors.get(0).getClass().isAssignableFrom(expected));
    }

    private void assertValue(Constraint<?> expected, String input) {
        ParsingResult<Constraint<?>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertFalse(ErrorUtils.printParseErrors(result.parseErrors), result.hasErrors());
        Assert.assertTrue(result.matched);
        Assert.assertEquals(expected, result.resultValue);
    }

    private void logInfo(ParsingResult<Constraint<?>> result) {
        LOG.info(sink.toString());
        if (result.hasErrors()) {
            LOG.info(ErrorUtils.printParseErrors(result.parseErrors));
        } else if (result.matched) {
            LOG.info("NodeTree: " + ParseTreeUtils.printNodeTree(result) + '\n');
        }
    }

    @Before
    public void setUp() {
        sink = new StringBuilderSink();
        parseRunner = new TracingParseRunner<Constraint<?>>(parser.root()).withLog(sink);
    }

    @Test()
    public void testEquals() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.EQUALS, 1);
        String input = "first.second==1";

        assertValue(expected, input);
    }

    @Test()
    public void testGreaterEquals() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.GREATER_EQUALS,
                1);
        String input = "first.second=ge=1";

        assertValue(expected, input);
    }

    @Test()
    public void testGreaterThan() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.GREATER_THAN, 1);
        String input = "first.second=gt=1";

        assertValue(expected, input);
    }

    @Test()
    public void testIntersects() {
        Comparable<?>[] args = { 1.34f, 2.4, "300" };

        Constraint<List<Comparable<?>>> expected = new Constraint<List<Comparable<?>>>("second", Comparison.INTERSECT,
                Arrays.asList(args));
        String input = "second=ix=[1.34f, 2.4d, '300']";

        assertValue(expected, input);
    }

    @Test()
    public void testLessEquals() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.LESS_EQUALS, 1l);
        String input = "first.second=le=1l";

        assertValue(expected, input);
    }

    @Test()
    public void testLessThan() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.LESS_THAN, 1l);
        String input = "first.second=lt=1L";

        assertValue(expected, input);
    }

    @Test()
    public void testNear() {
        Integer[] args = { 1, 2, 3, 4 };
        Arrays.asList(args);
        Constraint<List<Integer>> expected = new Constraint<List<Integer>>("second", Comparison.NEAR,
                Arrays.asList(args));
        String input = "second=nr=[1, 2, 3, 4]";

        assertValue(expected, input);
    }

    @Test()
    public void testNotEquals() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.NOT_EQUALS, 1l);
        String input = "first.second!=1L";

        assertValue(expected, input);
    }

    @Test()
    public void testOneSelector() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first", Comparison.EQUALS, 1);
        String input = "first==1";

        assertValue(expected, input);
    }

    @Test()
    public void testSingleArgArray() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("second", Comparison.NEAR, 1);
        String input = "second=nr=[1]";

        assertValue(expected, input);
    }

    @Test()
    public void testMultipleArgArray() {
        List<Integer> args = new ArrayList<>();
        args.add(1);
        args.add(2);
        Constraint<List<Integer>> expected = new Constraint<>("second", Comparison.IN, args);
        String input = "second=in=[1,2]";

        assertValue(expected, input);
    }

    @Test()
    public void testThreeSelector() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second.third", Comparison.EQUALS, 1l);
        String input = "first.second.third==1l";

        assertValue(expected, input);
    }

    @Test()
    public void testTwoSelector() {
        Constraint<Comparable<?>> expected = new Constraint<Comparable<?>>("first.second", Comparison.EQUALS, 1);
        String input = "first.second==1";

        assertValue(expected, input);
    }

    @Test()
    public void testUnknownSelector() {
        String input = "unknown==1";

        ParsingResult<Constraint<?>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        Assert.assertFalse(result.matched);
    }

    @Test()
    public void testWithin() {
        Comparable<?>[] args = { true, 's', null };

        Constraint<List<Comparable<?>>> expected = new Constraint<List<Comparable<?>>>("second", Comparison.WITHIN,
                Arrays.asList(args));
        String input = "second=wi=[true,'s',null]";

        assertValue(expected, input);
    }

    @Test()
    public void testZeroArg() {
        String input = "second=nr=";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testZeroArgArray() {
        String input = "second=nr=[]";

        assertError(InvalidInputError.class, input);
    }

}
