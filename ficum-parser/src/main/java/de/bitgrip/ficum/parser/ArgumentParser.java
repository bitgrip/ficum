package de.bitgrip.ficum.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

@BuildParseTree
public class ArgumentParser extends BaseParser<Object> {

    private static final Set<Class<? extends Comparable<?>>> baseTypes = new HashSet<Class<? extends Comparable<?>>>();

    static {
        baseTypes.add(Character.class);
        baseTypes.add(String.class);
        baseTypes.add(Float.class);
        baseTypes.add(Double.class);
        baseTypes.add(Integer.class);
        baseTypes.add(Long.class);
        baseTypes.add(Boolean.class);
        baseTypes.add(Calendar.class);
        baseTypes.add(UUID.class);
    }

    protected static final DateTimeFormatter ISO8601_TIMESTAMP = ISODateTimeFormat.dateTime().withOffsetParsed()
            .withChronology(GJChronology.getInstance());

    protected static final DateTimeFormatter ISO8601_DATE = ISODateTimeFormat.yearMonthDay()
            .withChronology(GJChronology.getInstance(DateTimeZone.UTC));

    public static Collection<Class<? extends Comparable<?>>> getBaseTypes() {
        return Collections.unmodifiableCollection(baseTypes);
    }

    @MemoMismatches
    protected Rule AlgebraicSign() {
        return AnyOf("-+");
    }

    protected Rule AnyString(final StringVar literal) {
        return Sequence(NoneOf("'"), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                literal.append(match());
                return true;
            }
        });
    }

    protected Rule Argument() {
        return Sequence(FirstOf(UUIDLiteral(), StringLiteral(), IntegerLiteral(), DoubleLiteral(), FloatLiteral(), DateLiteral(),
                TimestampLiteral(), BooleanTrue(), BooleanFalse(), NullLiteral()), new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        Comparable<?> argument = context.getValueStack().peek();
                        return isBaseType(argument);
                    }

                });
    }

    @SuppressSubnodes
    protected Rule BooleanFalse() {
        return Sequence(FirstOf(Sequence(AnyOf("Ff"), String("alse")), Sequence(AnyOf("Nn"), String("o"))),
                push(Boolean.FALSE));
    }

    @SuppressSubnodes
    protected Rule BooleanTrue() {
        return Sequence(FirstOf(Sequence(AnyOf("Tt"), String("rue")), Sequence(AnyOf("Yy"), String("es"))),
                push(Boolean.TRUE));
    }

    @SuppressSubnodes
    protected Rule DateLiteral() {
        return Sequence(Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(),
                Digit(), TestNot(Ch('T'))), new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            LocalDate parse = ISO8601_DATE.parseLocalDate(match());
                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            cal.clear();
                            cal.setTimeInMillis(parse.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis());
                            return push(cal);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

    protected Rule DecimalNumeral() {
        return FirstOf(Ch('0'), Sequence(CharRange('1', '9'), ZeroOrMore(Digit())));
    }

    @MemoMismatches
    protected Rule Digit() {
        return CharRange('0', '9');
    }

    protected Rule DoubleDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()),
                        Optional(DoubleMarker())),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(DoubleMarker())),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(DoubleMarker())),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), DoubleMarker()));
    }

    @SuppressSubnodes
    protected Rule DoubleLiteral() {
        return Sequence(Sequence(Optional(AlgebraicSign()), DoubleDecimal(), TestNot(Sign())),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            Double valueOf = Double.parseDouble(match());
                            return push(valueOf);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                });
    }

    @MemoMismatches
    protected Rule DoubleMarker() {
        return AnyOf("dD");
    }

    protected Rule Exponent() {
        return Sequence(AnyOf("eE"), Optional(AlgebraicSign()), OneOrMore(Digit()));
    }

    protected Rule FloatDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()),
                        Optional(FloatingPointMarker())),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(FloatingPointMarker())),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(FloatingPointMarker())),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), FloatingPointMarker()));
    }

    protected Rule FloatingPointMarker() {
        return AnyOf("fF");
    }

    @SuppressSubnodes
    protected Rule FloatLiteral() {
        return Sequence(Sequence(Optional(AlgebraicSign()), FloatDecimal(), TestNot(Sign())),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            Float valueOf = Float.parseFloat(match());
                            return push(valueOf);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                });
    }

    protected Rule HexDigit() {
      return FirstOf(LowerHexChar(), UpperHexChar(), Digit());
    }

    protected Rule LowerHexDigit(){
      return FirstOf(LowerHexChar(), Digit());
    }

    @SuppressSubnodes
    protected Rule HexEscape(final StringVar literal) {
        return Sequence(
                Sequence(FirstOf(Ch('#'), Sequence(Ch('0'), AnyOf("xX"))), OneOrMore(Sequence(HexDigit(), HexDigit()))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        literal.append(new String(Character.toChars(Integer.decode(match()).intValue())));
                        return true;
                    }
                });
    }

    @SuppressSubnodes
    protected Rule IntegerLiteral() {
        return Sequence(Sequence(Optional(AlgebraicSign()), DecimalNumeral(), Optional(AnyOf("lL")), TestNot(Sign())),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            boolean isLong = false;
                            String match = match();
                            int index = match.indexOf("l");
                            if (index == -1) {
                                index = match.indexOf("L");
                            }
                            if (index != -1) {
                                isLong = true;
                                match = match.substring(0, index);
                            }
                            index = match.indexOf("+");
                            if (index == 0) {
                                match = match.substring(1, match.length());
                            }
                            if (isLong) {
                                Long valueOf = Long.parseLong(match);
                                return push(valueOf);
                            } else {
                                Integer valueOf = Integer.parseInt(match);
                                return push(valueOf);
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                });
    }

    protected boolean isBaseType(Comparable<?> type) {
        if (type == null) {
            return true;
        }

        Class<?> clazz = type.getClass();
        for (Class<?> baseType : baseTypes) {
            if (baseType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @SuppressSubnodes
    protected Rule NullLiteral() {
        return Sequence(Sequence(AnyOf("Nn"), String("ull")), push(null));
    }

    protected Rule PctDigit() {
        return FirstOf(UpperHexChar(), Digit());
    }

    @SuppressSubnodes
    protected Rule PctEncoded(final StringVar literal) {
        return Sequence(OneOrMore(Sequence(Ch('%'), PctDigit(), PctDigit())), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                try {
                    literal.append(URLDecoder.decode(match(), "utf-8"));
                    return true;
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
            }
        });
    }

    public Rule root() {
        return Sequence(Argument(), EOI);
    }

    protected Rule Sign() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), Digit(), AnyOf("-+."));
    }

    /**
     * UUID format: b2cc307c-eb6d-4aca-bc0c-64a7c2f49c86
     *              8 Hexdigits '-' 4 Hexdigits '-' 4 Hexdigits '-' 4 Hexdigits '-' 12 Hexdigits
     * @return
     */
    protected Rule UUIDLiteral() {
      return Sequence(Sequence(NTimes(8, LowerHexDigit()), Ch('-'), NTimes(4, LowerHexDigit()), Ch('-'),
              NTimes(4, LowerHexDigit()), Ch('-'), NTimes(4, LowerHexDigit()), Ch('-'), NTimes(12, LowerHexDigit())),
              new Action<Comparable<?>>() {
                public boolean run(Context<Comparable<?>> context) {
                  try {
                    UUID uuid = UUID.fromString(match());
                    return push(uuid);
                  } catch (Exception e) {
                    return false;
                  }
                }
              });
    }

    protected Rule StringLiteral() {
        final StringVar literal = new StringVar();
        return Sequence(Sequence(Ch('\''),
                ZeroOrMore(FirstOf(PctEncoded(literal), HexEscape(literal), AnyString(literal))), Ch('\'')),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        String value = literal.get();
                        if (value == null) {
                            return push("");
                        }
                        if (value.length() == 1) {
                            return push(new Character(value.charAt(0)));
                        }
                        return push(value);

                    }
                });
    }

    @SuppressSubnodes
    protected Rule TimestampLiteral() {
        return Sequence(
                Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(), Digit(),
                        Ch('T'), Digit(), Digit(), Ch(':'), Digit(), Digit(), Ch(':'), Digit(), Digit(),
                        Ch('.'), Digit(), Digit(), Digit(), FirstOf(Ch('Z'), Sequence(AlgebraicSign(), Digit(), Digit(),
                                Ch(':'), Digit(), Digit(), Optional(Ch(':'), Digit(), Digit())))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            DateTime parse = ISO8601_TIMESTAMP.parseDateTime(match());
                            Calendar cal = Calendar.getInstance(parse.getZone().toTimeZone());
                            cal.clear();
                            cal.setTimeInMillis(parse.getMillis());
                            return push(cal);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

    @MemoMismatches
    protected Rule UpperHexChar() {
        return CharRange('A', 'F');
    }

    @MemoMismatches
    protected Rule LowerHexChar(){
      return CharRange('a', 'f');
    }

}
