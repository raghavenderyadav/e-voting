/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

package uk.dsxt.voting.common.utils;


import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class CryptoKeysGenerator {

    public static KeyPair[] generateKeys(CryptoHelper cryptoHelper, int count) throws Exception{
        KeyPair[] keys = new KeyPair[count];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = generateKeyPair(cryptoHelper);
        }
        return keys;
    }

    public static KeyPair generateKeyPair(CryptoHelper cryptoHelper) throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(cryptoHelper.getAlgoritm());
        keyGen.initialize(512);
        final java.security.KeyPair pair = keyGen.generateKeyPair();
        String pubKey = savePublicKey(cryptoHelper, pair.getPublic());
        String privateKey = savePrivateKey(cryptoHelper, pair.getPrivate());
        return new KeyPair(pubKey, privateKey);
    }

    public static String savePrivateKey(CryptoHelper cryptoHelper, PrivateKey privateKey) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance(cryptoHelper.getAlgoritm());
        PKCS8EncodedKeySpec spec = fact.getKeySpec(privateKey, PKCS8EncodedKeySpec.class);
        byte[] packed = spec.getEncoded();
        String key64 = Base64.getEncoder().encodeToString(packed);
        Arrays.fill(packed, (byte) 0);
        return key64;
    }

    public static String savePublicKey(CryptoHelper cryptoHelper, PublicKey publicKey) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance(cryptoHelper.getAlgoritm());
        X509EncodedKeySpec spec = fact.getKeySpec(publicKey, X509EncodedKeySpec.class);
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }
}
