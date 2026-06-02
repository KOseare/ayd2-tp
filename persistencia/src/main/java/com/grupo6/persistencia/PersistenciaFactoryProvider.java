package com.grupo6.persistencia;

import com.grupo6.environment.Environment;
import com.grupo6.persistencia.json.JsonPersistenciaFactory;
import com.grupo6.persistencia.textfile.TextFilePersistenciaFactory;
import com.grupo6.persistencia.xml.XmlPersistenciaFactory;

public final class PersistenciaFactoryProvider {

  private PersistenciaFactoryProvider() {}

  public static PersistenciaFactory createFromEnvironment() {
    final String type = Environment.persistenciaTipo.toUpperCase();
    if ("TEXT_FILE".equals(type)) {
      return new TextFilePersistenciaFactory(Environment.persistenciaDirectorio);
    }
    if ("JSON".equals(type)) {
      return new JsonPersistenciaFactory(Environment.persistenciaDirectorio);
    }
    if ("XML".equals(type)) {
      return new XmlPersistenciaFactory(Environment.persistenciaDirectorio);
    }
    throw new IllegalArgumentException("Unsupported persistencia type: " + type);
  }
}
