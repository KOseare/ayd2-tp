package com.grupo6.persistencia.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.IOException;
import java.util.Optional;

public class JsonStationsEntidad implements StationsEntidad {

  private final JsonDiskHelper disk;
  private final String filePath;

  public JsonStationsEntidad(JsonDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String stationsBlock) throws IOException {
    final JsonObject root = new JsonObject();
    final JsonArray stations = new JsonArray();
    final String block = stationsBlock == null ? "" : stationsBlock;
    for (String line : block.split("\n", -1)) {
      if (!line.isEmpty()) {
        stations.add(line);
      }
    }
    root.add("stations", stations);
    disk.writeRoot(filePath, root);
  }

  @Override
  public Optional<String> load() throws IOException {
    final JsonObject root = disk.readRoot(filePath);
    if (!root.has("stations") || !root.get("stations").isJsonArray()) {
      return Optional.empty();
    }
    final JsonArray stations = root.getAsJsonArray("stations");
    if (stations.isEmpty()) {
      return Optional.of("");
    }
    final StringBuilder block = new StringBuilder();
    for (int i = 0; i < stations.size(); i++) {
      if (i > 0) {
        block.append('\n');
      }
      block.append(stations.get(i).getAsString());
    }
    return Optional.of(block.toString());
  }
}
