/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dmr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class JsonStringParser {
    final TrackingReader reader;

    JsonStringParser(final TrackingReader reader) {
        this.reader = reader;
    }

    ModelNode parseDocument() throws IOException {
        final ModelNode modelNode = parseModelNode();
        consumeTrailingWhitespace();
        return modelNode;
    }

    ModelNode parseModelNode() throws IOException {
        int cp = reader.readNonWhiteSpace();
        switch (cp) {
            case '+':
            case '-':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9': {
                reader.unget(cp);
                return parseNumeric();
            }
            case 'I': {
                requireString("nfinity");
                requireNonWord();
                return new ModelNode(Double.POSITIVE_INFINITY);
            }
            case 'N': {
                requireString("aN");
                requireNonWord();
                return new ModelNode(Double.NaN);
            }
            case 'b': {
                cp = reader.requireCodePoint();
                if (cp == 'i') {
                    requireString("g");
                    if (! Character.isWhitespace(cp = reader.requireCodePoint())) {
                        throw unexpectedCharacter(cp);
                    }
                    cp = reader.requireNonWhiteSpace();
                    if (cp == 'd') {
                        requireString("ecimal");
                        requireNonWord();
                        return parseBigDecimalValue();
                    } else if (cp == 'i') {
                        requireString("nteger");
                        requireNonWord();
                        return parseBigIntegerValue();
                    } else {
                        throw unexpectedCharacter(cp);
                    }
                } else if (cp == 'y') {
                    requireString("tes");
                    requireNonWord();
                    cp = reader.requireNonWhiteSpace();
                    if (cp != '{') {
                        throw unexpectedCharacter(cp);
                    }
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (;;) {
                        cp = reader.requireNonWhiteSpace();
                        switch (cp) {
                            case '}': return new ModelNode(baos.toByteArray());
                            case '+': {
                                baos.write(Integer.decode(parseNumericWord()).intValue());
                                break;
                            }
                            case '-': {
                                baos.write(-Integer.decode(parseNumericWord()).intValue());
                                break;
                            }
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9': {
                                reader.unget(cp);
                                baos.write(Integer.decode(parseNumericWord()).intValue());
                                break;
                            }
                            default: {
                                throw unexpectedCharacter(cp);
                            }
                        }
                        cp = reader.requireNonWhiteSpace();
                        if (cp == ',') {
                            continue;
                        } else if (cp == '}') {
                            return new ModelNode(baos.toByteArray());
                        } else {
                            throw unexpectedCharacter(cp);
                        }
                    }
                }
            }
            case 'e': {
                requireString("xpression");
                cp = reader.requireNonWhiteSpace();
                if (cp != '"') {
                    throw unexpectedCharacter(cp);
                }
                return new ModelNode(new ValueExpression(parseStringContent()));
            }
            case 'f': {
                requireString("alse");
                requireNonWord();
                return new ModelNode(false);
            }
            case 't': {
                requireString("rue");
                requireNonWord();
                return new ModelNode(true);
            }
            case '"': {
                return new ModelNode(parseStringContent());
            }
            case '(': {
                return parsePropertyContent();
            }
            case '{': {
                return parseObjectContent();
            }
            case '[': {
                return parseListContent();
            }
            case -1: {
                return new ModelNode();
            }
            default: {
                throw unexpectedCharacter(cp);
            }
        }
    }

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[-+]?(?:0x[0-9a-fA-F]+L?|[0-9]+(?:L|\\.[0-9]+(?:[eE][-+]?[0-9]+)?)?)");

    private String parseNumericWord() throws IOException {
        final StringBuilder b = new StringBuilder();
        // first CP can follow whitespace
        int cp = reader.requireNonWhiteSpace();
        switch (cp) {
            case '+': {
                b.append('+');
                cp = reader.readCodePoint();
                switch (cp) {
                    case 'I': {
                        requireString("nfinity");
                        requireNonWord();
                        return "Infinity";
                    }
                    case 'N': {
                        requireString("aN");
                        requireNonWord();
                        return "NaN";
                    }
                    default: {
                        if (cp != -1) {
                            b.appendCodePoint(cp);
                            break;
                        }
                    }
                }
                break;
            }
            case '-': {
                b.append('-');
                cp = reader.readCodePoint();
                switch (cp) {
                    case 'I': {
                        requireString("nfinity");
                        requireNonWord();
                        return "-Infinity";
                    }
                    case 'N': {
                        requireString("aN");
                        requireNonWord();
                        return "NaN";
                    }
                    default: {
                        if (cp != -1) {
                            b.appendCodePoint(cp);
                            break;
                        }
                    }
                }
                break;
            }
            case 'I': {
                requireString("nfinity");
                requireNonWord();
                return "Infinity";
            }
            case 'N': {
                requireString("aN");
                requireNonWord();
                return "NaN";
            }
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9': {
                b.appendCodePoint(cp);
                break;
            }
            default: {
                throw unexpectedCharacter(cp);
            }
        }
        cp = reader.readCodePoint();
        loop: for (;;) {
            switch (cp) {
                case 'x':
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case '.': case '+': case '-': case 'L':
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9': {
                    b.appendCodePoint(cp);
                    cp = reader.readCodePoint();
                    continue loop;
                }
                default: {
                    if (Character.charCount(cp) > 1) {
                        throw unexpectedCharacter(cp);
                    }
                    reader.unget(cp);
                    break loop;
                }
            }
        }
        final String string = b.toString();
        if (NUMERIC_PATTERN.matcher(string).matches()) {
            requireNonWord();
            return string;
        }
        throw invalidNumber(string);
    }

    private ModelNode parseNumeric() throws IOException {
        String s = parseNumericWord();
        final ModelNode node;
        if (s.indexOf('.') != -1) {
            node = new ModelNode(Double.parseDouble(s));
        } else if (s.indexOf('L') == s.length() - 1) {
            node = new ModelNode(Long.decode(s.substring(0, s.length() - 1)).longValue());
        } else {
            node = new ModelNode(Integer.decode(s).intValue());
        }
        requireNonWord();
        return node;
    }

    private void requireNonWord() throws IOException {
        // don't use cp here because we have to unget if it isn't whitespace
        int cp = reader.read();
        if (cp == -1) {
            return;
        }
        if (! Character.isWhitespace(cp)) {
            if (Character.isJavaIdentifierPart(cp)) {
                throw unexpectedCharacter(cp);
            }
            reader.unget(cp);
        }
    }

    private ModelNode parseBigIntegerValue() throws IOException {
        return new ModelNode(new BigInteger(parseNumericWord()));
    }

    private ModelNode parseBigDecimalValue() throws IOException {
        return new ModelNode(new BigDecimal(parseNumericWord()));
    }

    private void requireString(final String string) throws IOException {
        int cp;
        int idx = 0;
        for (;;) {
            if (idx == string.length()) {
                return;
            }
            cp = string.codePointAt(idx);
            if (reader.requireCodePoint() != cp) {
                throw unexpectedCharacter(cp);
            }
            idx = string.offsetByCodePoints(idx, 1);
        }
    }

    private ModelNode parsePropertyContent() throws IOException {
        final ModelNode node = new ModelNode();
        int cp = reader.requireNonWhiteSpace();
        if (cp != '"') {
            throw unexpectedCharacter(cp);
        }
        final String key = parseStringContent();
        requireArrow();
        node.set(key, parseModelNode());
        if (reader.requireNonWhiteSpace() != ')') {
            throw unexpectedCharacter(cp);
        }
        return node;
    }

    private ModelNode parseObjectContent() throws IOException {
        int cp;
        final ModelNode node = new ModelNode();
        node.setEmptyObject();
        for (;;) {
            cp = reader.requireNonWhiteSpace();
            switch (cp) {
                case '}': {
                    return node;
                }
                case '"': {
                    final ModelNode nodeTarget = node.get(parseStringContent());
                    requireArrow();
                    nodeTarget.setNoCopy(parseModelNode());
                    switch ((cp = reader.requireNonWhiteSpace())) {
                        case ',': break;
                        case '}': return node;
                        default: {
                            throw unexpectedCharacter(cp);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedCharacter(cp);
                }
            }
        }
    }

    private ModelNode parseListContent() throws IOException {
        int cp;
        final ModelNode node = new ModelNode();
        node.setEmptyList();
        for (;;) {
            cp = reader.requireNonWhiteSpace();
            switch (cp) {
                case ']': {
                    return node;
                }
                default: {
                    if (Character.charCount(cp) > 1) {
                        throw unexpectedCharacter(cp);
                    }
                    // not a surrogate pair for sure, cast right to character
                    reader.unget(cp);
                    node.addNoCopy(parseModelNode());
                    cp = reader.requireNonWhiteSpace();
                    switch (cp) {
                        case ',': break;
                        case ']': return node;
                        default: {
                            throw unexpectedCharacter(cp);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void requireArrow() throws IOException {
        int cp;
        cp = reader.requireNonWhiteSpace();
        if (cp != '=') {
            throw unexpectedCharacter(cp);
        }
        cp = reader.requireCodePoint();
        if (cp != '>') {
            throw unexpectedCharacter(cp);
        }
        return;
    }

    private void consumeTrailingWhitespace() throws IOException {
        int cp = reader.readNonWhiteSpace();
        if (cp == -1) {
            return;
        }
        throw unexpectedCharacter(cp);
    }

    private IOException unexpectedCharacter(final int cp) {
        return new IOException(String.format("Unexpected character '%c' encountered at line %d, column %d", Integer.valueOf(cp), Integer.valueOf(reader.getLine()), Integer.valueOf(reader.getColumn())));
    }

    private IOException invalidNumber(final String str) {
        return new IOException(String.format("Invalid number '%s' encountered ending at line %d, column %d", str, Integer.valueOf(reader.getLine()), Integer.valueOf(reader.getColumn())));
    }

    private String parseStringContent() throws IOException {
        int cp;
        StringBuilder b = new StringBuilder();
        for (;;) {
            cp = reader.requireCodePoint();
            switch (cp) {
                case '"': {
                    return b.toString();
                }
                case '\\': {
                    b.appendCodePoint(reader.requireCodePoint());
                    continue;
                }
                default: {
                    b.appendCodePoint(cp);
                    continue;
                }
            }
        }
    }
}
