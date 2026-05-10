package com.grupo6.modelo;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FilaTurnos {

  private final Deque<Turno> enEspera = new ArrayDeque<>();
  private final Set<String> dniEnEspera = new HashSet<>();
  private final Map<String, Turno> turnoPorEstacion = new HashMap<>();
  private final List<Turno> turnosLlamados = new ArrayList<>();
  private int nextTurnId = 1;

  public boolean existeClienteEnFila(Cliente cliente) {
    return dniEnEspera.contains(cliente.getDni());
  }

  public boolean estaClienteEnAtencion(Cliente cliente) {
    String dni = cliente.getDni();
    for (Turno t : turnoPorEstacion.values()) {
      if (dni.equals(t.getDni())) {
        return true;
      }
    }
    return false;
  }

  public Turno registrarCliente(Cliente cliente) {
    Turno t = new Turno(nextTurnId++, cliente, new Timestamp(System.currentTimeMillis()));
    enEspera.addLast(t);
    dniEnEspera.add(cliente.getDni());
    return t;
  }

  public int obtenerCantidadTurnos() {
    return enEspera.size();
  }

  public List<Turno> obtenerTurnosLlamados(int n) {
    if (n <= 0) {
      return Collections.emptyList();
    }
    int from = Math.max(0, turnosLlamados.size() - n);
    return new ArrayList<>(turnosLlamados.subList(from, turnosLlamados.size()));
  }

  public NuevoLlamado llamarSiguienteEnEstacion(String estacion) {
    Turno enEstacion = turnoPorEstacion.get(estacion);
    if (enEspera.isEmpty()) {
      if (enEstacion == null) {
        return NuevoLlamado.sinPendientes();
      }
      return NuevoLlamado.sinPendientesMantenerActual(enEstacion.getDni());
    }

    Turno reemplazado = enEstacion;
    if (reemplazado != null) {
      turnoPorEstacion.remove(estacion);
      reemplazado.setEstacion(null);
    }

    Turno siguiente = enEspera.removeFirst();
    dniEnEspera.remove(siguiente.getDni());

    siguiente.setEstado(EstadoTurno.Llamado);
    siguiente.setEstacion(estacion);
    siguiente.setNroLlamados(0);
    turnoPorEstacion.put(estacion, siguiente);
    turnosLlamados.add(siguiente);

    return NuevoLlamado.asignado(reemplazado, siguiente);
  }

  public Renotificacion renotificarEnEstacion(String estacion) {
    Turno t = turnoPorEstacion.get(estacion);
    if (t == null) {
      return Renotificacion.sinActivo();
    }
    t.incrementNroLlamados();
    int intentos = t.getNroLlamados();
    if (intentos >= 3) {
      turnoPorEstacion.remove(estacion);
      t.setEstacion(null);
      return Renotificacion.removidoPorLimite(t.getDni());
    }
    return Renotificacion.notificado(t.getDni(), intentos);
  }

  public Optional<Turno> finalizarEnEstacion(String estacion) {
    Turno t = turnoPorEstacion.remove(estacion);
    if (t == null) {
      return Optional.empty();
    }
    t.setEstado(EstadoTurno.Atendido);
    t.setEstacion(null);
    return Optional.of(t);
  }

  public synchronized String exportStateBlob() {
    StringBuilder sb = new StringBuilder(256);
    sb.append("NEXT:").append(nextTurnId).append('\n');
    sb.append("QUEUE:");
    appendTurnRecords(sb, new ArrayList<>(enEspera), '~');
    sb.append('\n');
    sb.append("MAP:");
    boolean first = true;
    for (Map.Entry<String, Turno> e : turnoPorEstacion.entrySet()) {
      if (!first) {
        sb.append('~');
      }
      first = false;
      sb.append(escapePipe(e.getKey())).append('=').append(encodeTurn(e.getValue()));
    }
    sb.append('\n');
    sb.append("HIST:");
    appendTurnRecords(sb, turnosLlamados, '~');
    sb.append('\n');
    return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  public synchronized void replaceStateFromBlob(String base64Blob) {
    String raw;
    try {
      raw = new String(Base64.getDecoder().decode(base64Blob), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid replication snapshot encoding", e);
    }
    String[] lines = raw.split("\n", -1);
    if (lines.length < 4) {
      throw new IllegalArgumentException("Invalid replication snapshot");
    }
    int next = -1;
    String queuePart = null;
    String mapPart = null;
    String histPart = null;
    for (String line : lines) {
      if (line.startsWith("NEXT:")) {
        next = Integer.parseInt(line.substring("NEXT:".length()).trim());
      } else if (line.startsWith("QUEUE:")) {
        queuePart = line.substring("QUEUE:".length());
      } else if (line.startsWith("MAP:")) {
        mapPart = line.substring("MAP:".length());
      } else if (line.startsWith("HIST:")) {
        histPart = line.substring("HIST:".length());
      }
    }
    if (next < 0 || queuePart == null || mapPart == null || histPart == null) {
      throw new IllegalArgumentException("Incomplete replication snapshot");
    }
    nextTurnId = next;
    enEspera.clear();
    dniEnEspera.clear();
    turnoPorEstacion.clear();
    turnosLlamados.clear();
    if (!queuePart.isEmpty()) {
      for (String rec : queuePart.split("~", -1)) {
        if (rec.isEmpty()) {
          continue;
        }
        Turno t = decodeTurn(rec);
        enEspera.addLast(t);
        dniEnEspera.add(t.getDni());
      }
    }
    if (!mapPart.isEmpty()) {
      for (String entry : mapPart.split("~", -1)) {
        if (entry.isEmpty()) {
          continue;
        }
        String[] kv = entry.split("=", 2);
        if (kv.length != 2) {
          throw new IllegalArgumentException("Invalid MAP entry: " + entry);
        }
        String station = unescapePipe(kv[0]);
        Turno t = decodeTurn(kv[1]);
        turnoPorEstacion.put(station, t);
      }
    }
    if (!histPart.isEmpty()) {
      for (String rec : histPart.split("~", -1)) {
        if (rec.isEmpty()) {
          continue;
        }
        turnosLlamados.add(decodeTurn(rec));
      }
    }
  }

  private static void appendTurnRecords(StringBuilder sb, List<Turno> turns, char sep) {
    for (int i = 0; i < turns.size(); i++) {
      if (i > 0) {
        sb.append(sep);
      }
      sb.append(encodeTurn(turns.get(i)));
    }
  }

  private static String encodeTurn(Turno t) {
    final String est = t.getEstacion() == null ? "" : escapePipe(t.getEstacion());
    return new StringBuilder()
        .append(t.getId())
        .append('|')
        .append(escapePipe(t.getDni()))
        .append('|')
        .append(t.getEstado().name())
        .append('|')
        .append(t.getNroLlamados())
        .append('|')
        .append(est)
        .append('|')
        .append(t.getRegistro().getTime())
        .toString();
  }

  private static Turno decodeTurn(String rec) {
    String[] p = rec.split("\\|", -1);
    if (p.length != 6) {
      throw new IllegalArgumentException("Invalid turn record: " + rec);
    }
    int id = Integer.parseInt(p[0]);
    String dni = unescapePipe(p[1]);
    EstadoTurno estado = EstadoTurno.valueOf(p[2]);
    int nro = Integer.parseInt(p[3]);
    String estacionRaw = p[4];
    long ts = Long.parseLong(p[5]);
    Turno t = new Turno(id, new Cliente(dni), new Timestamp(ts));
    t.setEstado(estado);
    t.setNroLlamados(nro);
    if (!estacionRaw.isEmpty()) {
      t.setEstacion(unescapePipe(estacionRaw));
    }
    return t;
  }

  private static String escapePipe(String s) {
    return s.replace("\\", "\\\\").replace("|", "\\|");
  }

  private static String unescapePipe(String s) {
    StringBuilder out = new StringBuilder(s.length());
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
