package com.grupo6.persistencia;

import java.io.IOException;
import java.util.Optional;

public interface EstadoPersistencia {

  void save(String snapshotLine) throws IOException;

  Optional<String> load() throws IOException;
}
