package com.grupo6.persistencia.entidad;

import java.io.IOException;
import java.util.Optional;

public interface HistEntidad {

  void save(String histLine) throws IOException;

  Optional<String> load() throws IOException;
}
