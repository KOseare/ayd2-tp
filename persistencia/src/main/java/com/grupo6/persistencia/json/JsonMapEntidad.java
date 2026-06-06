package com.grupo6.persistencia.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grupo6.persistencia.entidad.MapEntidad;
import java.io.IOException;
import java.util.Optional;

public class JsonMapEntidad implements MapEntidad {

  private final JsonDiskHelper disk;
  private final String filePath;

  public JsonMapEntidad(JsonDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String mapLine) throws IOException {
    if (mapLine == null || !mapLine.startsWith("MAP:")) {
      throw new IllegalArgumentException("Map line must start with MAP:");
    }
    final String mapPart = mapLine.substring("MAP:".length());
    final JsonObject root = new JsonObject();
    final JsonArray assignments = new JsonArray();
    if (!mapPart.isEmpty()) {
      for (String entry : mapPart.split("~", -1)) {
        if (entry.isEmpty()) {
          continue;
        }
        final String[] kv = entry.split("=", 2);
        if (kv.length != 2) {
          throw new IllegalArgumentException("Invalid MAP entry: " + entry);
        }
        final JsonObject assignment = new JsonObject();
        assignment.addProperty("station", unescapePipe(kv[0]));
        assignment.add("turn", JsonTurnCodec.lineRecordToJson(kv[1]));
        assignments.add(assignment);
      }
    }
    root.add("assignments", assignments);
    disk.writeRoot(filePath, root);
  }

  @Override
  public Optional<String> load() throws IOException {
    final JsonObject root = disk.readRoot(filePath);
    if (!root.has("assignments") || !root.get("assignments").isJsonArray()) {
      return Optional.empty();
    }
    final JsonArray assignments = root.getAsJsonArray("assignments");
    final StringBuilder line = new StringBuilder("MAP:");
    for (int i = 0; i < assignments.size(); i++) {
      if (i > 0) {
        line.append('~');
      }
      final JsonObject assignment = assignments.get(i).getAsJsonObject();
      line.append(escapePipe(assignment.get("station").getAsString()));
      line.append('=');
      line.append(JsonTurnCodec.jsonToLineRecord(assignment.getAsJsonObject("turn")));
    }
    return Optional.of(line.toString());
  }

  private static String escapePipe(String s) {
    return s.replace("\\", "\\\\").replace("|", "\\|");
  }

  private static String unescapePipe(String s) {
    final StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        i++;
        if (i >= s.length()) {
          break;
        }
        out.append(s.charAt(i));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
