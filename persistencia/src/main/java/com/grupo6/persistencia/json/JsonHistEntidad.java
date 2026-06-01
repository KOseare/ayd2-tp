package com.grupo6.persistencia.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grupo6.persistencia.entidad.HistEntidad;
import java.io.IOException;
import java.util.Optional;

public class JsonHistEntidad implements HistEntidad {

  private final JsonDiskHelper disk;
  private final String filePath;

  public JsonHistEntidad(JsonDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String histLine) throws IOException {
    if (histLine == null || !histLine.startsWith("HIST:")) {
      throw new IllegalArgumentException("Hist line must start with HIST:");
    }
    final String histPart = histLine.substring("HIST:".length());
    final JsonObject root = new JsonObject();
    final JsonArray history = new JsonArray();
    if (!histPart.isEmpty()) {
      for (String rec : histPart.split("~", -1)) {
        if (!rec.isEmpty()) {
          history.add(JsonTurnCodec.lineRecordToJson(rec));
        }
      }
    }
    root.add("history", history);
    disk.writeRoot(filePath, root);
  }

  @Override
  public Optional<String> load() throws IOException {
    final JsonObject root = disk.readRoot(filePath);
    if (!root.has("history") || !root.get("history").isJsonArray()) {
      return Optional.empty();
    }
    final JsonArray history = root.getAsJsonArray("history");
    final StringBuilder line = new StringBuilder("HIST:");
    for (int i = 0; i < history.size(); i++) {
      if (i > 0) {
        line.append('~');
      }
      line.append(JsonTurnCodec.jsonToLineRecord(history.get(i).getAsJsonObject()));
    }
    return Optional.of(line.toString());
  }
}
