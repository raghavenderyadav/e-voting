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

import java.io.UnsupportedEncodingException;
import java.security.*;

public class MockCryptoHelper implements CryptoHelper {

    private final PublicKey publicKey;
    
    private final PrivateKey privateKey;
    
    public MockCryptoHelper() {
        publicKey = new PublicKey() {
            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        };
        privateKey = new PrivateKey() {
            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        };
    }

    @Override
    public PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
        return privateKey;
    }

    @Override
    public PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
        return publicKey;
    }

    @Override
    public String createSignature(String originalText, PrivateKey privateKey) {
        return "SIGN";
    }

    @Override
    public boolean verifySignature(String originalText, String signature, PublicKey publicKey) {
        return true;
    }

    @Override
    public String encrypt(String text, PublicKey key) throws GeneralSecurityException, UnsupportedEncodingException {
        return text;
    }

    @Override
    public String decrypt(String cipherText, PrivateKey key) throws GeneralSecurityException, UnsupportedEncodingException {
        return cipherText;
    }
    
    @Override
    public String getDigest(String text) throws NoSuchAlgorithmException {
        return "text";
    }
}
