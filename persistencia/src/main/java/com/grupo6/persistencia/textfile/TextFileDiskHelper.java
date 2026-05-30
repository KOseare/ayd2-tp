package com.grupo6.persistencia.textfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TextFileDiskHelper {

  public List<String> readLines(String filePath) throws IOException {
    final File target = new File(filePath);
    if (!target.isFile() || target.length() == 0) {
      return null;
    }
    final List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(target))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  public Optional<String> readContent(String filePath) throws IOException {
    final List<String> lines = readLines(filePath);
    if (lines == null || lines.isEmpty()) {
      return Optional.empty();
    }
    final StringBuilder content = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      if (i > 0) {
        content.append('\n');
      }
      content.append(lines.get(i));
    }
    return Optional.of(content.toString());
  }

  public void writeLines(String filePath, List<String> lines) throws IOException {
    final StringBuilder content = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      if (i > 0) {
        content.append('\n');
      }
      content.append(lines.get(i));
    }
    writeContent(filePath, content.toString());
  }

  public void writeContent(String filePath, String content) throws IOException {
    if (content == null) {
      throw new IllegalArgumentException("Content must not be null");
    }
    final File target = new File(filePath);
    final File parent = target.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    final File temp = new File(target.getAbsolutePath() + ".tmp");
    try (FileWriter writer = new FileWriter(temp)) {
      writer.write(content);
      if (!content.endsWith("\n")) {
        writer.write('\n');
      }
    }
    Files.move(
        temp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }
}
