package com.grupo6.environment;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class Environment {
  final private static Dotenv env = Dotenv.load();
  public static String MONITOR_HOST = env.get("MONITOR_HOST");
  public static int MONITOR_PORT = Integer.parseInt(env.get("MONITOR_PORT"));
  public static String OPERATOR_HOST = env.get("OPERATOR_HOST");
  public static int OPERATOR_PORT = Integer.parseInt(env.get("OPERATOR_PORT"));
}
