package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.PersistenciaFactory;
import com.grupo6.persistencia.entidad.HistEntidad;
import com.grupo6.persistencia.entidad.MapEntidad;
import com.grupo6.persistencia.entidad.QueueEntidad;
import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.File;

public class TextFilePersistenciaFactory extends PersistenciaFactory {

  private static final String STATIONS_FILE = "stations.txt";
  private static final String QUEUE_FILE = "queue.txt";
  private static final String MAP_FILE = "map.txt";
  private static final String HIST_FILE = "hist.txt";

  private final TextFileDiskHelper disk;
  private final String baseDirectory;

  public TextFilePersistenciaFactory(String baseDirectory) {
    if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
      throw new IllegalArgumentException("Persistence base directory must not be empty");
    }
    this.baseDirectory = baseDirectory.trim();
    this.disk = new TextFileDiskHelper();
  }

  @Override
  public StationsEntidad createStationsEntidad() {
    return new TextFileStationsEntidad(disk, resolvePath(STATIONS_FILE));
  }

  @Override
  public QueueEntidad createQueueEntidad() {
    return new TextFileQueueEntidad(disk, resolvePath(QUEUE_FILE));
  }

  @Override
  public MapEntidad createMapEntidad() {
    return new TextFileMapEntidad(disk, resolvePath(MAP_FILE));
  }

  @Override
  public HistEntidad createHistEntidad() {
    return new TextFileHistEntidad(disk, resolvePath(HIST_FILE));
  }

  private String resolvePath(String fileName) {
    return new File(baseDirectory, fileName).getPath();
  }
}
