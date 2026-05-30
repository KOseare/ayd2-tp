package com.grupo6.persistencia.entidad;

import java.io.IOException;
import java.util.Optional;

public interface MapEntidad {

  void save(String mapLine) throws IOException;

  Optional<String> load() throws IOException;
}
