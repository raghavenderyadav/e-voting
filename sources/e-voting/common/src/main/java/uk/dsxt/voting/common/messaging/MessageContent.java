/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package uk.dsxt.voting.common.messaging;

import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MessageContent {

    private static final String FIELD_TYPE = "TYPE";
    private static final String FIELD_AUTHOR = "AUTHOR";
    private static final String FIELD_SIGN = "SIGN";
    private static final String FIELD_TIMESTAMP = "TIMESTAMP";
    public static final String FIELD_UID = "UID";

    private static final String CHARSET = "UTF-8";

    private final Map<String, String> fields = new HashMap<>();

    public MessageContent(byte[] body) throws IllegalArgumentException {
        String contentString = new String(body, Charset.forName(CHARSET));
        String[] terms = contentString.split(";");
        for(String term : terms) {
            String[] keyValuePair = term.split("=");
            if (keyValuePair.length != 2)
                throw new IllegalArgumentException("Invalid message structure");
            fields.put(keyValuePair[0], descapeValue(keyValuePair[1]));
        }
        if (getField(FIELD_TYPE) == null)
            throw new IllegalArgumentException("Message does not contain TYPE field");
        if (getField(FIELD_AUTHOR) == null)
            throw new IllegalArgumentException("Message does not contain AUTHOR field");
        if (getField(FIELD_SIGN) == null)
            throw new IllegalArgumentException("Message does not contain SIGN field");
        if (getField(FIELD_UID) == null)
            throw new IllegalArgumentException("Message does not contain FIELD_UID field");
    }

    public boolean checkSign(PublicKey publicKey, CryptoHelper cryptoHelper) throws GeneralSecurityException, UnsupportedEncodingException {
        String contentString = buildContentWithoutSign(fields);
        return cryptoHelper.verifySignature(contentString, fields.get(FIELD_SIGN), publicKey);
    }

    public String getField(String fieldName) {
        return fields.get(fieldName);
    }

    public String getAuthor() {
        return getField(FIELD_AUTHOR);
    }

    public long getFieldTimestamp() {
        return Long.parseLong(getField(FIELD_TIMESTAMP));
    }

    public String getType() {
        return getField(FIELD_TYPE);
    }

    public String getUID() {
        return getField(FIELD_UID);
    }

    public static byte[] buildOutputMessage(String type, String authorId, PrivateKey privateKey, CryptoHelper cryptoHelper, Map<String, String> fields)
            throws GeneralSecurityException, UnsupportedEncodingException {
        fields.put(FIELD_TYPE, type);
        fields.put(FIELD_AUTHOR, authorId);
        fields.put(FIELD_TIMESTAMP, Long.toString(System.currentTimeMillis()));
        fields.put(FIELD_UID, UUID.randomUUID().toString());
        String contentString = buildContentWithoutSign(fields);
        String signature = cryptoHelper.createSignature(contentString, privateKey);
        contentString += String.format(";%s=%s", FIELD_SIGN, signature);
        return contentString.getBytes(CHARSET);
    }

    private static String buildContentWithoutSign(Map<String, String> fields) {
        return String.join(";", fields.entrySet().stream()
                .filter(e -> !FIELD_SIGN.equals(e.getKey()))
                .map(e -> String.format("%s=%s", e.getKey(), escapeValue(e.getValue())))
                .sorted().collect(Collectors.toList()));
    }

    private static String escapeValue(String value) {
        if (value == null)
            return "";
        value = value.replaceAll("!", "!o").replaceAll("=", "!e").replaceAll(";", "!p");
        return value;
    }

    private static String descapeValue(String value) {
        if (value == null)
            return "";
        return value.replaceAll("!e", "=").replaceAll("!p", ";").replaceAll("!o", "!");
    }

}
