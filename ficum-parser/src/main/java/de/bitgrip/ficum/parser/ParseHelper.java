package de.bitgrip.ficum.parser;

import java.util.Arrays;
import java.util.Deque;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import de.bitgrip.ficum.node.Builder;
import de.bitgrip.ficum.node.Node;

public class ParseHelper {

    protected ParseHelper() {
    }

    public static final Node parse(String query, String... allowedSelectorNames) {
        ExpressionParser parser = Parboiled.createParser(ExpressionParser.class, (Object) Arrays.copyOf(allowedSelectorNames, allowedSelectorNames.length));
        ReportingParseRunner<Deque<Object>> parseRunner = new ReportingParseRunner<Deque<Object>>(parser.root());
        ParsingResult<Deque<Object>> result = parseRunner.run(query);

        if (result.hasErrors()) {
            throw new IllegalArgumentException(ErrorUtils.printParseErrors(result.parseErrors));
        }
        return Builder.build(result.resultValue);
    }

}
