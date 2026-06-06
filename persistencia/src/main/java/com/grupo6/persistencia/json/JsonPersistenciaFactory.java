package com.grupo6.persistencia.json;

import com.grupo6.persistencia.PersistenciaFactory;
import com.grupo6.persistencia.entidad.HistEntidad;
import com.grupo6.persistencia.entidad.MapEntidad;
import com.grupo6.persistencia.entidad.QueueEntidad;
import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.File;

public class JsonPersistenciaFactory extends PersistenciaFactory {

  private static final String STATIONS_FILE = "stations.json";
  private static final String QUEUE_FILE = "queue.json";
  private static final String MAP_FILE = "map.json";
  private static final String HIST_FILE = "hist.json";

  private final JsonDiskHelper disk;
  private final String baseDirectory;

  public JsonPersistenciaFactory(String baseDirectory) {
    if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
      throw new IllegalArgumentException("Persistence base directory must not be empty");
    }
    this.baseDirectory = baseDirectory.trim();
    this.disk = new JsonDiskHelper();
  }

  @Override
  public StationsEntidad createStationsEntidad() {
    return new JsonStationsEntidad(disk, resolvePath(STATIONS_FILE));
  }

  @Override
  public QueueEntidad createQueueEntidad() {
    return new JsonQueueEntidad(disk, resolvePath(QUEUE_FILE));
  }

  @Override
  public MapEntidad createMapEntidad() {
    return new JsonMapEntidad(disk, resolvePath(MAP_FILE));
  }

  @Override
  public HistEntidad createHistEntidad() {
    return new JsonHistEntidad(disk, resolvePath(HIST_FILE));
  }

  private String resolvePath(String fileName) {
    return new File(baseDirectory, fileName).getPath();
  }
}
