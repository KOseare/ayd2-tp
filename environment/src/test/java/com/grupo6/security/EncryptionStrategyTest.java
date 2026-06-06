package com.grupo6.security;

import junit.framework.TestCase;

public class EncryptionStrategyTest extends TestCase {
  public void testAesEncryptsAndDecryptsDni() {
    EncryptionStrategy strategy = new AESEncryptionStrategy("test-secret");

    String encrypted = strategy.encrypt("12345678");

    assertFalse("12345678".equals(encrypted));
    assertEquals("12345678", strategy.decrypt(encrypted));
  }

  public void testAesUsesStableCipherTextForDuplicateChecks() {
    EncryptionStrategy strategy = new AESEncryptionStrategy("test-secret");

    String first = strategy.encrypt("12345678");
    String second = strategy.encrypt("12345678");

    assertEquals(first, second);
  }

  public void testNoEncryptionPassesTextThrough() {
    EncryptionStrategy strategy = new NoEncryptionStrategy();

    assertEquals("12345678", strategy.encrypt("12345678"));
    assertEquals("12345678", strategy.decrypt("12345678"));
  }
}
