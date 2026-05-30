package com.grupo6.environment;

import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class Environment {
  private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();

  public static final int cantidadNodos = getInt("CANTIDAD_NODOS", 5);
  public static final String monitorHost = getString("MONITOR_HOST", "localhost");
  public static final int monitorPort = getInt("MONITOR_PORT", 3006);
  public static final String serverSecretKey = getString("SERVER_SECRET_KEY", "grupo6-default-secret-key");

  public static final List<ServerAddress> nodosServidores = getServidores();

  public static final String persistenciaTipo = getString("PERSISTENCIA_TIPO", "TEXT_FILE");
  public static final String persistenciaDirectorio =
      getString("PERSISTENCIA_DIR", "./data/persistencia");

  private static List<ServerAddress> getServidores() {
    final int nqty = getInt("CANTIDAD_NODOS", 3);
    final List<ServerAddress> r = new ArrayList<ServerAddress>();
    for (int i = 0; i < nqty; i++) {
      final String host = "SERVER_HOST_" + i;
      final String port = "SERVER_PORT_" + i;
      r.add(new ServerAddress(getString(host, "localhost"), getInt(port, 0)));
    }
    return r;
  }

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
