package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.EstadoPersistencia;
import com.grupo6.persistencia.PersistenciaFactory;

public class TextFilePersistenciaFactory extends PersistenciaFactory {

  private final String filePath;

  public TextFilePersistenciaFactory(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("Persistencia file path must not be empty");
    }
    this.filePath = filePath.trim();
  }

  @Override
  public EstadoPersistencia createEstadoPersistencia() {
    return new TextFileEstadoPersistencia(filePath);
  }
}
