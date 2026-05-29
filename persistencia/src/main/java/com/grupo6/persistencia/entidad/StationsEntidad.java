package com.grupo6.persistencia.entidad;

import java.io.IOException;
import java.util.Optional;

public interface StationsEntidad {

  void save(String stationsBlock) throws IOException;

  Optional<String> load() throws IOException;
}
