/*
// $Id: XmlaOlap4jShaEncoder.java 315 2010-05-29 00:56:11Z jhyde $
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2007-2010 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package org.olap4j.driver.olap4ld.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA encoder to create unique hash strings for cache elements.
 *
 * @author Luc Boudreau
 * @version $Id: XmlaOlap4jShaEncoder.java 315 2010-05-29 00:56:11Z jhyde $
 */
class XmlaOlap4jShaEncoder {

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String encodeSha1(String text) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e1) {
                throw new RuntimeException(e1);
            }
        }

        byte[] sha1hash = new byte[40];

        md.update(text.getBytes(), 0, text.length());

        sha1hash = md.digest();

        return convertToHex(sha1hash);
    }
}

// End XmlaOlap4jShaEncoder.java
