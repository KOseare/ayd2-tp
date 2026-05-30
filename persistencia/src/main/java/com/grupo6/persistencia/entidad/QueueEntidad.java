package com.grupo6.persistencia.entidad;

import java.io.IOException;
import java.util.Optional;

public interface QueueEntidad {

  void save(String queueBlock) throws IOException;

  Optional<String> load() throws IOException;
}
