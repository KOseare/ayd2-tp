package com.grupo6.security;

import com.grupo6.environment.Environment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryptionStrategy implements EncryptionStrategy {
  private static final String AES = "AES";
  private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
  private final SecretKeySpec secretKey;

  public AESEncryptionStrategy() {
    this(Environment.serverSecretKey);
  }

  public AESEncryptionStrategy(String secret) {
    this.secretKey = buildKey(secret);
  }

  @Override
  public String encrypt(String plainText) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  @Override
  public String decrypt(String cipherText) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decoded = Base64.getDecoder().decode(cipherText);
      byte[] plain = cipher.doFinal(decoded);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Decryption failed", e);
    }
  }

  private static SecretKeySpec buildKey(String secret) {
    try {
      MessageDigest sha = MessageDigest.getInstance("SHA-256");
      byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(Arrays.copyOf(key, 16), AES);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Key creation failed", e);
    }
  }
}
