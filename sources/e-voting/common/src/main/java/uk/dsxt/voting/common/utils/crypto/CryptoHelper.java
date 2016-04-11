package uk.dsxt.voting.common.utils.crypto;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface CryptoHelper {
    PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException;

    PublicKey loadPublicKey(String stored) throws GeneralSecurityException;

    String createSignature(String originalText, PrivateKey privateKey)
        throws GeneralSecurityException, UnsupportedEncodingException;

    boolean verifySignature(String originalText, String signature, PublicKey publicKey)
            throws GeneralSecurityException, UnsupportedEncodingException;

    String encrypt(String text, PublicKey key) throws GeneralSecurityException, UnsupportedEncodingException;

    String decrypt(String cipherText, PrivateKey key) throws GeneralSecurityException, UnsupportedEncodingException;

    String getDigest(String text) throws NoSuchAlgorithmException;
}
