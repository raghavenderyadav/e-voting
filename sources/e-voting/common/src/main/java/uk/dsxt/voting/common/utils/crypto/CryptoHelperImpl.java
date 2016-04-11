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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class CryptoHelperImpl implements CryptoHelper {
    private static final String ENCODING = "UTF-8";

    private final String asymmetricAlgorithm;

    private final String symmetricAlgorithm;

    private final String signatureAlgorithm;

    private final String digestAlgorithm;

    private final int asymmetricKeyLength;

    private final int symmetricKeyLength;

    private final SecureRandom random = new SecureRandom();

    public static final CryptoHelperImpl DEFAULT_CRYPTO_HELPER = new CryptoHelperImpl("RSA", "AES", "SHA1WithRSA", "SHA1", 2048, 128);

    public CryptoHelperImpl(String asymmetricAlgorithm, String symmetricAlgorithm, String signatureAlgorithm, String digestAlgorithm, int asymmetricKeyLength, int symmetricKeyLength) {
        this.asymmetricAlgorithm = asymmetricAlgorithm;
        this.symmetricAlgorithm = symmetricAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.digestAlgorithm = digestAlgorithm;
        this.asymmetricKeyLength = asymmetricKeyLength;
        this.symmetricKeyLength = symmetricKeyLength;
    }

    @Override
    public PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
        byte[] clear = Base64.getDecoder().decode(key64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
        PrivateKey priv = fact.generatePrivate(keySpec);
        Arrays.fill(clear, (byte) 0);
        return priv;
    }

    @Override
    public PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
        byte[] data = Base64.getDecoder().decode(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
        return fact.generatePublic(spec);
    }

    @Override
    public String createSignature(String originalText, PrivateKey privateKey)
        throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] data = originalText.getBytes(ENCODING);
        Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initSign(privateKey);
        sig.update(data);
        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    @Override
    public boolean verifySignature(String originalText, String signature, PublicKey publicKey)
        throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] data = originalText.getBytes(ENCODING);
        Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initVerify(publicKey);
        sig.update(data);

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return sig.verify(signatureBytes);
    }

    @Override
    public String encrypt(String text, PublicKey key) throws GeneralSecurityException, UnsupportedEncodingException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(symmetricAlgorithm);
        keyGenerator.init(symmetricKeyLength, random);
        SecretKey secretKey = keyGenerator.generateKey();

        Cipher keyCipher = Cipher.getInstance(asymmetricAlgorithm);
        keyCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedKey = keyCipher.doFinal(secretKey.getEncoded());

        Cipher textCipher = Cipher.getInstance(symmetricAlgorithm);
        textCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedText = textCipher.doFinal(text.getBytes());
        return String.format("%s@%s", new String(Base64.getEncoder().encode(encryptedKey)), new String(Base64.getEncoder().encode(encryptedText)));
    }

    @Override
    public String decrypt(String cipherText, PrivateKey key) throws GeneralSecurityException, UnsupportedEncodingException {
        String[] keyAndText = cipherText.split("@");

        Cipher keyCipher = Cipher.getInstance(asymmetricAlgorithm);
        keyCipher.init(Cipher.DECRYPT_MODE, key);
        byte[] cipherKeyBytes = Base64.getDecoder().decode(keyAndText[0].getBytes());
        byte[] decryptedKeyBytes = keyCipher.doFinal(cipherKeyBytes);
        SecretKey secretKey = new SecretKeySpec(decryptedKeyBytes, symmetricAlgorithm);

        Cipher textCipher = Cipher.getInstance(symmetricAlgorithm);
        textCipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] cipherTextBytes = Base64.getDecoder().decode(keyAndText[1].getBytes());
        byte[] decryptedTextBytes = textCipher.doFinal(cipherTextBytes);
        return new String(decryptedTextBytes);
    }
    
    @Override
    public String getDigest(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
        digest.update(text.getBytes());
        return new String(Base64.getEncoder().encode(digest.digest()));
    }

    public CryptoKeysGenerator createCryptoKeysGenerator() {
        return new CryptoKeysGenerator(asymmetricAlgorithm, asymmetricKeyLength);
    }
}
