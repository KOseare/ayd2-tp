package com.grupo6.persistencia;

import com.grupo6.environment.Environment;
import com.grupo6.persistencia.textfile.TextFilePersistenciaFactory;

public final class PersistenciaFactoryProvider {

  private PersistenciaFactoryProvider() {}

  public static PersistenciaFactory createFromEnvironment() {
    final String type = Environment.persistenciaTipo.toUpperCase();
    if ("TEXT_FILE".equals(type)) {
      return new TextFilePersistenciaFactory(Environment.persistenciaRuta);
    }
    throw new IllegalArgumentException("Unsupported persistencia type: " + type);
  }
}
