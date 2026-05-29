package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.entidad.QueueEntidad;
import java.io.IOException;
import java.util.Optional;

public class TextFileQueueEntidad implements QueueEntidad {

  private final TextFileDiskHelper disk;
  private final String filePath;

  public TextFileQueueEntidad(TextFileDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String queueBlock) throws IOException {
    if (queueBlock == null || queueBlock.trim().isEmpty()) {
      throw new IllegalArgumentException("Queue block must not be empty");
    }
    disk.writeContent(filePath, queueBlock);
  }

  @Override
  public Optional<String> load() throws IOException {
    return disk.readContent(filePath);
  }
}
