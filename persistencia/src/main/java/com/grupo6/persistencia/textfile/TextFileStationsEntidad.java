package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.IOException;
import java.util.Optional;

public class TextFileStationsEntidad implements StationsEntidad {

  private final TextFileDiskHelper disk;
  private final String filePath;

  public TextFileStationsEntidad(TextFileDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String stationsBlock) throws IOException {
    disk.writeContent(filePath, stationsBlock == null ? "" : stationsBlock);
  }

  @Override
  public Optional<String> load() throws IOException {
    return Optional.empty();
    /*
    return disk.readContent(filePath);
    */
  }
}
