package org.toilelibre.libe.curl;

import java.io.*;
import java.math.*;
import java.security.spec.*;

final class DerReader {
    static class Asn1Object {
        private static final byte LOWER_5_BITS = (byte) 0x1F;
        private final int         tag;
        private final int         type;

        private final byte []     value;

        Asn1Object (final int tag, final byte [] value) {
            this.tag = tag;
            this.type = tag & Asn1Object.LOWER_5_BITS;
            this.value = value;
        }

        BigInteger getInteger () throws IOException {
            if (this.type != DerReader.INTEGER) {
                throw new IOException ("Invalid DER: object is not integer");
            }

            return new BigInteger (this.value);
        }

        KeySpec getKeySpec () throws IOException {

            final DerReader parser = this.getReader ();

            parser.read ();
            final BigInteger modulus = parser.read ().getInteger ();
            final BigInteger publicExp = parser.read ().getInteger ();
            final BigInteger privateExp = parser.read ().getInteger ();
            final BigInteger prime1 = parser.read ().getInteger ();
            final BigInteger prime2 = parser.read ().getInteger ();
            final BigInteger exp1 = parser.read ().getInteger ();
            final BigInteger exp2 = parser.read ().getInteger ();
            final BigInteger crtCoef = parser.read ().getInteger ();

            return new RSAPrivateCrtKeySpec (modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
        }

        DerReader getReader () throws IOException {
            if (!this.isConstructed ()) {
                throw new IOException ("Invalid DER: can't parse primitive entity");
            }

            return new DerReader (this.value);
        }

        boolean isConstructed () {
            return (this.tag & DerReader.CONSTRUCTED) == DerReader.CONSTRUCTED;
        }
    }


    private static final int   BYTE_MAX            = 0xFF;
    private static final int   CONSTRUCTED         = 0x20;
    private static final int   INTEGER             = 0x02;
    private static final byte  LOWER_7_BITS        = (byte) 0x7F;
    private static final int   MAX_NUMBER_OF_BYTES = 4;

    private final InputStream in;

    DerReader (final byte [] bytes) {
        this (new ByteArrayInputStream (bytes));
    }

    private DerReader (final InputStream in) {
        this.in = in;
    }

    private int getLength () throws IOException {

        final int i = this.in.read ();
        if (i == -1) {
            throw new IOException ("Invalid DER: length missing");
        }

        if ((i & ~DerReader.LOWER_7_BITS) == 0) {
            return i;
        }

        final int num = i & DerReader.LOWER_7_BITS;

        if ((i >= DerReader.BYTE_MAX) || (num > DerReader.MAX_NUMBER_OF_BYTES)) {
            throw new IOException ("Invalid DER: length field too big (" + i + ")");
        }

        final byte [] bytes = new byte [num];
        final int n = this.in.read (bytes);
        if (n < num) {
            throw new IOException ("Invalid DER: length too short");
        }

        return new BigInteger (1, bytes).intValue ();
    }

    Asn1Object read () throws IOException {
        final int tag = this.in.read ();

        if (tag == -1) {
            throw new IOException ("Invalid DER: stream too short, missing tag");
        }

        final int length = this.getLength ();

        final byte [] value = new byte [length];
        final int n = this.in.read (value);
        if (n < length) {
            throw new IOException ("Invalid DER: stream too short, missing value");
        }

        return new Asn1Object (tag, value);
    }
}
