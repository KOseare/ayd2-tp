package com.grupo6.security;

public class NoEncryptionStrategy implements EncryptionStrategy {
  @Override
  public String encrypt(String plainText) {
    return plainText;
  }

  @Override
  public String decrypt(String cipherText) {
    return cipherText;
  }
}
