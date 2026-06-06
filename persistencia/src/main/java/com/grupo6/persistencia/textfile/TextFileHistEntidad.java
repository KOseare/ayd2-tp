package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.entidad.HistEntidad;
import java.io.IOException;
import java.util.Optional;

public class TextFileHistEntidad implements HistEntidad {

  private final TextFileDiskHelper disk;
  private final String filePath;

  public TextFileHistEntidad(TextFileDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String histLine) throws IOException {
    if (histLine == null || !histLine.startsWith("HIST:")) {
      throw new IllegalArgumentException("Hist line must start with HIST:");
    }
    disk.writeContent(filePath, histLine);
  }

  @Override
  public Optional<String> load() throws IOException {
    return disk.readContent(filePath);
  }
}
