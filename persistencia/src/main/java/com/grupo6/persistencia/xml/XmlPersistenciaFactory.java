package com.grupo6.persistencia.xml;

import com.grupo6.persistencia.PersistenciaFactory;
import com.grupo6.persistencia.entidad.HistEntidad;
import com.grupo6.persistencia.entidad.MapEntidad;
import com.grupo6.persistencia.entidad.QueueEntidad;
import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.File;

public class XmlPersistenciaFactory extends PersistenciaFactory {

  private static final String STATIONS_FILE = "stations.xml";
  private static final String QUEUE_FILE = "queue.xml";
  private static final String MAP_FILE = "map.xml";
  private static final String HIST_FILE = "hist.xml";

  private final XmlDiskHelper disk;
  private final String baseDirectory;

  public XmlPersistenciaFactory(String baseDirectory) {
    if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
      throw new IllegalArgumentException("Persistence base directory must not be empty");
    }
    this.baseDirectory = baseDirectory.trim();
    this.disk = new XmlDiskHelper();
  }

  @Override
  public StationsEntidad createStationsEntidad() {
    return new XmlStationsEntidad(disk, resolvePath(STATIONS_FILE));
  }

  @Override
  public QueueEntidad createQueueEntidad() {
    return new XmlQueueEntidad(disk, resolvePath(QUEUE_FILE));
  }

  @Override
  public MapEntidad createMapEntidad() {
    return new XmlMapEntidad(disk, resolvePath(MAP_FILE));
  }

  @Override
  public HistEntidad createHistEntidad() {
    return new XmlHistEntidad(disk, resolvePath(HIST_FILE));
  }

  private String resolvePath(String fileName) {
    return new File(baseDirectory, fileName).getPath();
  }
}
