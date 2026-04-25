package com.grupo6.environment;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class Environment {
  private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();

  public static final String SERVER_HOST = getString("SERVER_HOST", "127.0.0.1");
  public static final int SERVER_PORT = getInt("SERVER_PORT", 7000);

  public static final String MONITOR_HOST = getString("MONITOR_HOST", SERVER_HOST);
  public static final int MONITOR_PORT = getInt("MONITOR_PORT", 3002);
  public static final String OPERATOR_HOST = getString("OPERATOR_HOST", SERVER_HOST);
  public static final int OPERATOR_PORT = getInt("OPERATOR_PORT", 3001);

  private static String getString(String key, String defaultValue) {
    String value = env.get(key);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    return value.trim();
  }

  private static int getInt(String key, int defaultValue) {
    String value = env.get(key);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }
}
