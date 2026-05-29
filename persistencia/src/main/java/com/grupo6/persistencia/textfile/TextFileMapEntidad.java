package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.entidad.MapEntidad;
import java.io.IOException;
import java.util.Optional;

public class TextFileMapEntidad implements MapEntidad {

  private final TextFileDiskHelper disk;
  private final String filePath;

  public TextFileMapEntidad(TextFileDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String mapLine) throws IOException {
    if (mapLine == null || !mapLine.startsWith("MAP:")) {
      throw new IllegalArgumentException("Map line must start with MAP:");
    }
    disk.writeContent(filePath, mapLine);
  }

  @Override
  public Optional<String> load() throws IOException {
    return disk.readContent(filePath);
  }
}
