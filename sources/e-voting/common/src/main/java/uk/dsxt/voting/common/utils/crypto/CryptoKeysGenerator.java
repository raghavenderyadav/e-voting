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

package uk.dsxt.voting.common.utils.crypto;


import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class CryptoKeysGenerator {

    private final String algorithm;

    private final int keyLength;
    
    public CryptoKeysGenerator(String algorithm, int keyLength) {
        this.algorithm = algorithm;
        this.keyLength = keyLength;
    }

    public KeyPair[] generateKeys(int count) throws Exception{
        KeyPair[] keys = new KeyPair[count];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = generateKeyPair();
        }
        return keys;
    }

    public KeyPair generateKeyPair() throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keyLength);
        final java.security.KeyPair pair = keyGen.generateKeyPair();
        String pubKey = savePublicKey(pair.getPublic());
        String privateKey = savePrivateKey(pair.getPrivate());
        return new KeyPair(pubKey, privateKey);
    }

    public String savePrivateKey(PrivateKey privateKey) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance(algorithm);
        PKCS8EncodedKeySpec spec = fact.getKeySpec(privateKey, PKCS8EncodedKeySpec.class);
        byte[] packed = spec.getEncoded();
        String key64 = Base64.getEncoder().encodeToString(packed);
        Arrays.fill(packed, (byte) 0);
        return key64;
    }

    public String savePublicKey(PublicKey publicKey) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec spec = fact.getKeySpec(publicKey, X509EncodedKeySpec.class);
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }
    
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]);
        CryptoKeysGenerator cryptoKeysGenerator = CryptoHelperImpl.DEFAULT_CRYPTO_HELPER.createCryptoKeysGenerator();
        for (int i = 0; i < count; i++) {
            KeyPair keyPair = cryptoKeysGenerator.generateKeyPair();
            System.out.printf("public: %s%nprivate: %s%n%n", keyPair.getPublicKey(), keyPair.getPrivateKey());
        }
    }
}
