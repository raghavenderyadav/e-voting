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

package uk.dsxt.voting.common;

import org.junit.Test;
import uk.dsxt.voting.common.utils.CryptoHelper;
import uk.dsxt.voting.common.utils.CryptoKeysGenerator;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CryptoTest {

    private final CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

    @Test
    public void testCryptoKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(cryptoHelper.getAlgoritm());
        KeyPair pair = gen.generateKeyPair();
        final String originalText = "Text to be encrypted";
        String signature = cryptoHelper.createSignature(originalText, pair.getPrivate());
        assertTrue(cryptoHelper.verifySignature(originalText, signature, pair.getPublic()));

        //convert public key to string
        String publicKey = CryptoKeysGenerator.savePublicKey(cryptoHelper, pair.getPublic());
        //convert public key back to Public key from string
        PublicKey publicSaved = cryptoHelper.loadPublicKey(publicKey);
        //convert private key to string
        String privateKey = CryptoKeysGenerator.savePrivateKey(cryptoHelper, pair.getPrivate());
        //convert private key back to Public key from string
        PrivateKey privateSaved = cryptoHelper.loadPrivateKey(privateKey);

        String signatureSaved = cryptoHelper.createSignature(originalText, privateSaved);
        assertTrue(cryptoHelper.verifySignature(originalText, signatureSaved, publicSaved));

        assertEquals(signature, signatureSaved);
    }
}
