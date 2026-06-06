package com.grupo6.persistencia.json;

import com.google.gson.JsonObject;

final class JsonTurnCodec {

  private JsonTurnCodec() {}

  static JsonObject lineRecordToJson(String rec) {
    final String[] p = rec.split("\\|", -1);
    if (p.length != 6) {
      throw new IllegalArgumentException("Invalid turn record: " + rec);
    }
    final JsonObject turn = new JsonObject();
    turn.addProperty("id", Integer.parseInt(p[0]));
    turn.addProperty("dni", unescapePipe(p[1]));
    turn.addProperty("estado", p[2]);
    turn.addProperty("nroLlamados", Integer.parseInt(p[3]));
    turn.addProperty("estacion", p[4].isEmpty() ? "" : unescapePipe(p[4]));
    turn.addProperty("registro", Long.parseLong(p[5]));
    return turn;
  }

  static String jsonToLineRecord(JsonObject turn) {
    final String estacion = turn.has("estacion") ? turn.get("estacion").getAsString() : "";
    return new StringBuilder()
        .append(turn.get("id").getAsInt())
        .append('|')
        .append(escapePipe(turn.get("dni").getAsString()))
        .append('|')
        .append(turn.get("estado").getAsString())
        .append('|')
        .append(turn.get("nroLlamados").getAsInt())
        .append('|')
        .append(estacion.isEmpty() ? "" : escapePipe(estacion))
        .append('|')
        .append(turn.get("registro").getAsLong())
        .toString();
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
