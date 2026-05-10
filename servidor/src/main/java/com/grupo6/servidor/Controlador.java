package com.grupo6.servidor;

import com.grupo6.modelo.Cliente;
import com.grupo6.modelo.FilaTurnos;
import com.grupo6.modelo.NuevoLlamado;
import com.grupo6.modelo.Renotificacion;
import com.grupo6.modelo.Turno;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Controlador {

  private final FilaTurnos fila = new FilaTurnos();
  private final Set<String> claimedStationIds = new HashSet<>();
  private final List<PrintWriter> monitorSubscribers = new ArrayList<>();
  private final List<PrintWriter> operatorSubscribers = new ArrayList<>();
  private final List<PrintWriter> replicaSubscribers = new ArrayList<>();
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  public void subscribeMonitor(PrintWriter writer, BufferedReader reader) throws IOException {
    synchronized (this) {
      monitorSubscribers.add(writer);
    }
    writer.println("OK|SUBSCRIBED");

    try {
      while (reader.readLine() != null) {
        // Keep stream open while monitor is connected.
      }
    } finally {
      synchronized (this) {
        monitorSubscribers.remove(writer);
      }
    }
  }

  public void subscribeOperator(PrintWriter writer, BufferedReader reader) throws IOException {
    synchronized (this) {
      operatorSubscribers.add(writer);
    }
    writer.println("OK|SUBSCRIBED");

    try {
      while (reader.readLine() != null) {
        // Keep stream open while monitor is connected.
      }
    } finally {
      synchronized (this) {
        operatorSubscribers.remove(writer);
      }
    }
  }

  public void subscribeReplica(PrintWriter writer, BufferedReader reader) throws IOException {
    synchronized (this) {
      replicaSubscribers.add(writer);
      writer.println("OK|SUBSCRIBED");
      writer.println(buildFullStateLine());
    }
    try {
      while (reader.readLine() != null) {
        // Keep stream open; passive servers do not send commands on this socket.
      }
    } finally {
      synchronized (this) {
        replicaSubscribers.remove(writer);
      }
    }
  }

  public synchronized void clearReplicaSubscribers() {
    replicaSubscribers.clear();
  }

  public synchronized void applyFullStateFromLeaderLine(String line) {
    if (line == null || !line.startsWith("STATE_FULL|")) {
      throw new IllegalArgumentException("Unexpected snapshot line");
    }
    String encoded = line.substring("STATE_FULL|".length());
    byte[] decoded = Base64.getDecoder().decode(encoded);
    String body = new String(decoded, StandardCharsets.UTF_8);
    int sep = body.indexOf("\n--\n");
    if (sep < 0) {
      throw new IllegalArgumentException("Malformed snapshot body");
    }
    String claimsBlock = body.substring(0, sep);
    String filaBlob = body.substring(sep + "\n--\n".length());
    claimedStationIds.clear();
    if (!claimsBlock.isEmpty()) {
      for (String s : claimsBlock.split("\n", -1)) {
        if (!s.isEmpty()) {
          claimedStationIds.add(s);
        }
      }
    }
    fila.replaceStateFromBlob(filaBlob.trim());
  }

  public synchronized void handleRegister(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_REGISTER");
      return;
    }
    String dni = parts[1].trim();
    if (dni.isEmpty() || !isValidDni(dni)) {
      writer.println("ERROR|INVALID_DNI");
      return;
    }
    Cliente cliente = new Cliente(dni);
    if (fila.existeClienteEnFila(cliente)) {
      writer.println("ERROR|ALREADY_IN_QUEUE");
      return;
    }
    if (fila.estaClienteEnAtencion(cliente)) {
      writer.println("ERROR|ALREADY_IN_ATTENTION");
      return;
    }
    fila.registrarCliente(cliente);
    broadcastToOperators("OK|QUEUE_SIZE|" + fila.obtenerCantidadTurnos());
    writer.println("OK|REGISTERED|" + dni);
    pushFullStateToReplicas();
  }

  public synchronized void handleClaimStation(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    if (stationId.isEmpty()) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    if (claimedStationIds.contains(stationId)) {
      writer.println("ERROR|STATION_ID_EXISTS");
      return;
    }
    claimedStationIds.add(stationId);
    writer.println("OK|STATION_CLAIMED|" + stationId);
    pushFullStateToReplicas();
  }

  public synchronized void handleReleaseStation(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    if (stationId.isEmpty()) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    claimedStationIds.remove(stationId);
    writer.println("OK|STATION_RELEASED|" + stationId);
    pushFullStateToReplicas();
  }

  public synchronized void handleGetQueueSize(PrintWriter writer) {
    writer.println("OK|QUEUE_SIZE|" + fila.obtenerCantidadTurnos());
  }

  public synchronized void handleCallNext(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    if (stationId.isEmpty()) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    NuevoLlamado r = fila.llamarSiguienteEnEstacion(stationId);
    switch (r.getTipo()) {
      case SIN_PENDIENTES:
        writer.println("OK|NO_PENDING");
        return;
      case SIN_PENDIENTES_MANTENER_ACTUAL:
        writer.println("ERROR|NO_PENDING_KEEPING_CURRENT|" + r.getDniActualMantenido().orElse(""));
        return;
      case ASIGNADO:
        r.getReemplazado()
            .ifPresent(
                previous -> broadcastToMonitors(
                    "EVENT|REMOVED|" + previous.getDni() + "|" + stationId));
        Turno asignado = r.getAsignado().orElse(null);
        if (asignado == null) {
          writer.println("OK|NO_PENDING");
          return;
        }
        writer.println("OK|CALLED|" + asignado.getDni());
        broadcastToMonitors("EVENT|CALL|" + asignado.getDni() + "|" + stationId);
        pushFullStateToReplicas();
        return;
      default:
        writer.println("ERROR|UNKNOWN_COMMAND");
    }
  }

  public synchronized void handleRenotify(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    Renotificacion r = fila.renotificarEnEstacion(stationId);
    switch (r.getTipo()) {
      case SIN_TURNO_ACTIVO:
        writer.println("ERROR|NO_ACTIVE_CLIENT");
        return;
      case NOTIFICADO:
        String dniN = r.getDni().orElse("");
        int intentos = r.getIntentos();
        broadcastToMonitors("EVENT|RENOTIFY|" + dniN + "|" + stationId + "|" + intentos);
        writer.println("OK|RENOTIFIED|" + dniN + "|" + intentos);
        pushFullStateToReplicas();
        return;
      case REMOVIDO_POR_LIMITE:
        String dniL = r.getDni().orElse("");
        writer.println("OK|REMOVED_BY_LIMIT|" + dniL);
        broadcastToMonitors("EVENT|REMOVED|" + dniL + "|" + stationId);
        pushFullStateToReplicas();
        return;
      default:
        writer.println("ERROR|UNKNOWN_COMMAND");
    }
  }

  public synchronized void handleFinalize(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    Optional<Turno> finalized = fila.finalizarEnEstacion(stationId);
    if (!finalized.isPresent()) {
      writer.println("ERROR|NO_ACTIVE_CLIENT");
      return;
    }
    String dni = finalized.get().getDni();
    writer.println("OK|FINALIZED|" + dni);
    broadcastToMonitors("EVENT|FINALIZED|" + dni + "|" + stationId);
    pushFullStateToReplicas();
  }

  private synchronized void broadcastToMonitors(String message) {
    List<PrintWriter> disconnected = new ArrayList<>();
    for (PrintWriter monitorWriter : monitorSubscribers) {
      monitorWriter.println(message);
      if (monitorWriter.checkError()) {
        disconnected.add(monitorWriter);
      }
    }
    monitorSubscribers.removeAll(disconnected);
  }

  private synchronized void broadcastToOperators(String message) {
    List<PrintWriter> disconnected = new ArrayList<>();
    for (PrintWriter writer : operatorSubscribers) {
      writer.println(message);
      if (writer.checkError()) {
        disconnected.add(writer);
      }
    }
    operatorSubscribers.removeAll(disconnected);
  }

  private String buildFullStateLine() {
    String claimsBlock =
        claimedStationIds.stream().sorted().collect(Collectors.joining("\n"));
    String body = claimsBlock + "\n--\n" + fila.exportStateBlob();
    return "STATE_FULL|"
        + Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
  }

  private void pushFullStateToReplicas() {
    if (replicaSubscribers.isEmpty()) {
      return;
    }
    String line = buildFullStateLine();
    List<PrintWriter> dead = new ArrayList<>();
    for (PrintWriter replicaWriter : replicaSubscribers) {
      replicaWriter.println(line);
      if (replicaWriter.checkError()) {
        dead.add(replicaWriter);
      }
    }
    replicaSubscribers.removeAll(dead);
  }

  private boolean isValidDni(String dni) {
    if (!NUMERIC_PATTERN.matcher(dni).matches()) {
      return false;
    }
    return dni.length() == 7 || dni.length() == 8;
  }
}
