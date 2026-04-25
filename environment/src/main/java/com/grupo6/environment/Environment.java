package com.grupo6.environment;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class Environment {
  final private static Dotenv env = Dotenv.load();
  public static String MONITOR_HOST = env.get("MONITOR_HOST");
  public static int MONITOR_PORT = Integer.parseInt(env.get("MONITOR_PORT"));
  public static String OPERATOR_HOST = env.get("OPERATOR_HOST");
  public static int OPERATOR_PORT = Integer.parseInt(env.get("OPERATOR_PORT"));

  private static String optional(String key, String defaultValue) {
    String v = env.get(key);
    if (v == null || v.trim().isEmpty()) {
      return defaultValue;
    }
    return v;
  }

  public static String QUEUE_HOST = optional("QUEUE_HOST", "127.0.0.1");
  public static int QUEUE_PORT = Integer.parseInt(optional("QUEUE_PORT", "9090"));
}
