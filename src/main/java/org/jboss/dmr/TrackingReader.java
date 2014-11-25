/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TrackingReader extends Reader {
    private final Reader delegate;

    private int line = 1, column = 1;

    private int unget = -1;

    TrackingReader(final Reader delegate) {
        this.delegate = delegate;
    }

    int getLine() {
        return line;
    }

    int getColumn() {
        return column;
    }

    int requireNonWhiteSpace() throws IOException {
        int cp;
        while (Character.isWhitespace(cp = requireCodePoint())) {}
        return cp;
    }

    int requireCodePoint() throws IOException {
        int cp = readCodePoint();
        if (cp == -1) {
            throw new EOFException("Unexpected end of input");
        }
        return cp;
    }

    int readNonWhiteSpace() throws IOException {
        int cp;
        while (Character.isWhitespace(cp = readCodePoint())) {}
        return cp;
    }

    int readCodePoint() throws IOException {
        int cp = read();
        if (cp == -1) {
            return -1;
        }
        if (Character.isHighSurrogate((char) cp)) {
            int c2 = read();
            return c2 == -1 ? 0xFFFD : Character.toCodePoint((char) cp, (char) c2);
        } else {
            return cp;
        }
    }

    public int read() throws IOException {
        int ch;
        if (unget != -1) {
            ch = unget;
            unget = -1;
        } else {
            ch = delegate.read();
        }
        if (ch == 10) {
            column = 1;
            line++;
        } else if (ch == -1) {
            return ch;
        } else {
            column++;
        }
        return ch;
    }

    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        boolean adj = false;
        if (unget != -1) try {
            cbuf[off] = (char) unget;
            adj = true;
        } finally {
            unget = -1;
        }
        int res = delegate.read(cbuf, adj ? off + 1 : off, len);
        if (res == -1) {
            return -1;
        }
        for (int i = 0; i < res; i ++) {
            int ch = cbuf[off + i];
            if (ch == 10) {
                column = 1;
                line ++;
            } else {
                column ++;
            }
        }
        return adj ? res + 1 : res;
    }

    void unget(int ch) {
        if (unget != -1) {
            throw new IllegalStateException("Pipe is clogged");
        }
        if (ch >= 0) {
            unget = ch;
        }
    }

    public void close() throws IOException {
        delegate.close();
    }
}
