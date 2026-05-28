package com.grupo6.persistencia.textfile;

import com.grupo6.persistencia.EstadoPersistencia;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class TextFileEstadoPersistencia implements EstadoPersistencia {

  private final String filePath;

  public TextFileEstadoPersistencia(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public void save(String snapshotLine) throws IOException {
    if (snapshotLine == null || snapshotLine.trim().isEmpty()) {
      throw new IllegalArgumentException("Snapshot line must not be empty");
    }
    final File target = new File(filePath);
    final File parent = target.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    final File temp = new File(target.getAbsolutePath() + ".tmp");
    try (FileWriter writer = new FileWriter(temp)) {
      writer.write(snapshotLine.trim());
      writer.write('\n');
    }
    Files.move(
        temp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }

  @Override
  public Optional<String> load() throws IOException {
    final File target = new File(filePath);
    if (!target.isFile() || target.length() == 0) {
      return Optional.empty();
    }
    final StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(target))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (content.length() > 0) {
          content.append('\n');
        }
        content.append(line);
      }
    }
    final String snapshot = content.toString().trim();
    if (snapshot.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(snapshot);
  }
}
