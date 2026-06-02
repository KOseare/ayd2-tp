package com.grupo6.persistencia.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class JsonDiskHelper {

  private static final Gson GSON = new Gson();

  public JsonObject readRoot(String filePath) throws IOException {
    final File target = new File(filePath);
    if (!target.isFile() || target.length() == 0) {
      return new JsonObject();
    }
    try (FileReader reader = new FileReader(target, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    }
  }

  public void writeRoot(String filePath, JsonObject root) throws IOException {
    if (root == null) {
      throw new IllegalArgumentException("Root must not be null");
    }
    final File target = new File(filePath);
    final File parent = target.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    final File temp = new File(target.getAbsolutePath() + ".tmp");
    try (FileWriter writer = new FileWriter(temp, StandardCharsets.UTF_8)) {
      GSON.toJson(root, writer);
      writer.write('\n');
    }
    Files.move(
        temp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }
}
