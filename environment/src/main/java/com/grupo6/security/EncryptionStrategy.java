package com.grupo6.security;

public interface EncryptionStrategy {
  String encrypt(String plainText);

  String decrypt(String cipherText);
}
