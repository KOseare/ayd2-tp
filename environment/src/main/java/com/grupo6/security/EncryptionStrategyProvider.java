package com.grupo6.security;

import com.grupo6.environment.Environment;

public abstract class EncryptionStrategyProvider {
  public static EncryptionStrategy fromEnvironment() throws Exception {
    final String chosen = Environment.encryptionMethod;
    switch (chosen.toUpperCase().trim()) {
      case "AES":
        return new AESEncryptionStrategy();
      case "NONE":
        return new NoEncryptionStrategy();
      default:
        throw new Exception("Método de encriptación no soportado.");
    }
  }
}
