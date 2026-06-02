package com.grupo6.persistencia.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grupo6.persistencia.entidad.QueueEntidad;
import java.io.IOException;
import java.util.Optional;

public class JsonQueueEntidad implements QueueEntidad {

  private final JsonDiskHelper disk;
  private final String filePath;

  public JsonQueueEntidad(JsonDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String queueBlock) throws IOException {
    if (queueBlock == null || queueBlock.trim().isEmpty()) {
      throw new IllegalArgumentException("Queue block must not be empty");
    }
    int next = -1;
    String queuePart = null;
    for (String line : queueBlock.split("\n", -1)) {
      if (line.startsWith("NEXT:")) {
        next = Integer.parseInt(line.substring("NEXT:".length()).trim());
      } else if (line.startsWith("QUEUE:")) {
        queuePart = line.substring("QUEUE:".length());
      }
    }
    if (next < 0 || queuePart == null) {
      throw new IllegalArgumentException("Queue block must contain NEXT and QUEUE lines");
    }
    final JsonObject root = new JsonObject();
    root.addProperty("next", next);
    final JsonArray queue = new JsonArray();
    if (!queuePart.isEmpty()) {
      for (String rec : queuePart.split("~", -1)) {
        if (!rec.isEmpty()) {
          queue.add(JsonTurnCodec.lineRecordToJson(rec));
        }
      }
    }
    root.add("queue", queue);
    disk.writeRoot(filePath, root);
  }

  @Override
  public Optional<String> load() throws IOException {
    final JsonObject root = disk.readRoot(filePath);
    if (!root.has("next") || !root.has("queue")) {
      return Optional.empty();
    }
    final StringBuilder block = new StringBuilder();
    block.append("NEXT:").append(root.get("next").getAsInt()).append('\n');
    block.append("QUEUE:");
    final JsonArray queue = root.getAsJsonArray("queue");
    for (int i = 0; i < queue.size(); i++) {
      if (i > 0) {
        block.append('~');
      }
      block.append(JsonTurnCodec.jsonToLineRecord(queue.get(i).getAsJsonObject()));
    }
    return Optional.of(block.toString());
  }
}
