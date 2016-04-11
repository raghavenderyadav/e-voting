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
import uk.dsxt.voting.common.utils.crypto.CryptoHelperImpl;
import uk.dsxt.voting.common.utils.crypto.CryptoKeysGenerator;
import uk.dsxt.voting.common.utils.crypto.KeyPair;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CryptoTest {

    private final CryptoHelperImpl cryptoHelper = CryptoHelperImpl.DEFAULT_CRYPTO_HELPER;

    @Test
    public void testSignature() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();
        final String originalText = "Text for signature";
        String signature = cryptoHelper.createSignature(originalText, cryptoHelper.loadPrivateKey(pair.getPrivateKey()));
        assertTrue(cryptoHelper.verifySignature(originalText, signature, cryptoHelper.loadPublicKey(pair.getPublicKey())));

        //convert public key to string
        String publicKey = pair.getPublicKey();
        //convert public key back to Public key from string
        PublicKey publicSaved = cryptoHelper.loadPublicKey(publicKey);
        //convert private key to string
        String privateKey = pair.getPrivateKey();
        //convert private key back to Public key from string
        PrivateKey privateSaved = cryptoHelper.loadPrivateKey(privateKey);

        String signatureSaved = cryptoHelper.createSignature(originalText, privateSaved);
        assertTrue(cryptoHelper.verifySignature(originalText, signatureSaved, publicSaved));

        assertEquals(signature, signatureSaved);
    }

    @Test
    public void testEncodingShort() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();
        final String originalText = "Text to be encrypted";
        String encryptedText = cryptoHelper.encrypt(originalText, cryptoHelper.loadPublicKey(pair.getPublicKey()));
        String decryptedText = cryptoHelper.decrypt(encryptedText, cryptoHelper.loadPrivateKey(pair.getPrivateKey()));
        assertEquals(originalText, decryptedText);

        //convert public key to string
        String publicKey = pair.getPublicKey();
        //convert public key back to Public key from string
        PublicKey publicSaved = cryptoHelper.loadPublicKey(publicKey);
        //convert private key to string
        String privateKey = pair.getPrivateKey();
        //convert private key back to Public key from string
        PrivateKey privateSaved = cryptoHelper.loadPrivateKey(privateKey);

        String encryptedTextSaved = cryptoHelper.encrypt(originalText, publicSaved);
        String decryptedTextSaved = cryptoHelper.decrypt(encryptedTextSaved, privateSaved);
        assertEquals(originalText, decryptedTextSaved);
    }

    @Test
    public void testEncodingLong() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();
        String originalText = "Text to be encrypted";
        for(int i = 0; i < 10; i++) {
            originalText = originalText + originalText;
        }
        String encryptedText = cryptoHelper.encrypt(originalText, cryptoHelper.loadPublicKey(pair.getPublicKey()));
        String decryptedText = cryptoHelper.decrypt(encryptedText, cryptoHelper.loadPrivateKey(pair.getPrivateKey()));
        assertEquals(originalText, decryptedText);

        //convert public key to string
        String publicKey = pair.getPublicKey();
        //convert public key back to Public key from string
        PublicKey publicSaved = cryptoHelper.loadPublicKey(publicKey);
        //convert private key to string
        String privateKey = pair.getPrivateKey();
        //convert private key back to Public key from string
        PrivateKey privateSaved = cryptoHelper.loadPrivateKey(privateKey);

        String encryptedTextSaved = cryptoHelper.encrypt(originalText, publicSaved);
        String decryptedTextSaved = cryptoHelper.decrypt(encryptedTextSaved, privateSaved);
        assertEquals(originalText, decryptedTextSaved);
    }
    @Test
    public void testDigest() throws Exception {
        assertEquals(cryptoHelper.getDigest("AAA"), cryptoHelper.getDigest("AAA"));
        assertNotEquals(cryptoHelper.getDigest("AAA"), cryptoHelper.getDigest("AAB"));
    }
}
