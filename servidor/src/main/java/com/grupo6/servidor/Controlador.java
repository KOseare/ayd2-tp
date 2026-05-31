package com.grupo6.servidor;

import com.grupo6.modelo.Cliente;
import com.grupo6.modelo.FilaTurnos;
import com.grupo6.modelo.NuevoLlamado;
import com.grupo6.modelo.Renotificacion;
import com.grupo6.modelo.Turno;
import com.grupo6.persistencia.entidad.HistEntidad;
import com.grupo6.persistencia.entidad.MapEntidad;
import com.grupo6.persistencia.entidad.QueueEntidad;
import com.grupo6.persistencia.entidad.StationsEntidad;
import com.grupo6.security.AESEncryptionStrategy;
import com.grupo6.security.EncryptionStrategy;
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
  private StationsEntidad stationsEntidad;
  private QueueEntidad queueEntidad;
  private MapEntidad mapEntidad;
  private HistEntidad histEntidad;
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  private final EncryptionStrategy encryptionStrategy;

  public Controlador() {
    this(new AESEncryptionStrategy());
  }

  public Controlador(EncryptionStrategy encryptionStrategy) {
    this.encryptionStrategy = encryptionStrategy;
  }

  public synchronized void setPersistenciaEntidades(
      StationsEntidad stationsEntidad,
      QueueEntidad queueEntidad,
      MapEntidad mapEntidad,
      HistEntidad histEntidad) {
    this.stationsEntidad = stationsEntidad;
    this.queueEntidad = queueEntidad;
    this.mapEntidad = mapEntidad;
    this.histEntidad = histEntidad;
  }

  public synchronized void clearPersistenciaEntidades() {
    setPersistenciaEntidades(null, null, null, null);
  }

  public synchronized boolean restorePersistedState() throws IOException {
    if (!hasPersistenciaEntidades()) {
      return false;
    }
    boolean restored = false;
    final Optional<String> stationsBlock = stationsEntidad.load();
    if (stationsBlock.isPresent()) {
      claimedStationIds.clear();
      for (String stationId : stationsBlock.get().split("\n", -1)) {
        if (!stationId.isEmpty()) {
          claimedStationIds.add(stationId);
        }
      }
      restored = true;
    }
    final StringBuilder filaPlain = new StringBuilder(256);
    final Optional<String> queueBlock = queueEntidad.load();
    if (queueBlock.isPresent()) {
      filaPlain.append(queueBlock.get().trim()).append('\n');
      restored = true;
    }
    final Optional<String> mapLine = mapEntidad.load();
    if (mapLine.isPresent()) {
      filaPlain.append(mapLine.get().trim()).append('\n');
      restored = true;
    }
    final Optional<String> histLine = histEntidad.load();
    if (histLine.isPresent()) {
      filaPlain.append(histLine.get().trim()).append('\n');
      restored = true;
    }
    if (filaPlain.length() > 0) {
      fila.replaceStateFromPlain(filaPlain.toString());
    }
    return restored;
  }

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
    String encryptedDni = parts[1].trim();
    String plainDni;
    try {
      plainDni = encryptionStrategy.decrypt(encryptedDni);
    } catch (RuntimeException e) {
      writer.println("ERROR|INVALID_DNI");
      return;
    }
    if (encryptedDni.isEmpty() || !isValidDni(plainDni)) {
      writer.println("ERROR|INVALID_DNI");
      return;
    }
    Cliente cliente = new Cliente(encryptedDni);
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
    writer.println("OK|REGISTERED|" + encryptedDni);
    commitState();
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
    commitState();
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
    commitState();
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
        commitState();
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
        commitState();
        return;
      case REMOVIDO_POR_LIMITE:
        String dniL = r.getDni().orElse("");
        writer.println("OK|REMOVED_BY_LIMIT|" + dniL);
        broadcastToMonitors("EVENT|REMOVED|" + dniL + "|" + stationId);
        commitState();
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
    commitState();
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
    String claimsBlock = claimedStationIds.stream().sorted().collect(Collectors.joining("\n"));
    String body = claimsBlock + "\n--\n" + fila.exportStateBlob();
    return "STATE_FULL|"
        + Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
  }

  private void commitState() {
    pushFullStateToReplicas();
    persistCurrentState();
  }

  private void persistCurrentState() {
    if (!hasPersistenciaEntidades()) {
      return;
    }
    try {
      final String stationsBlock = claimedStationIds.stream().sorted().collect(Collectors.joining("\n"));
      stationsEntidad.save(stationsBlock);
      queueEntidad.save(fila.exportNextLine() + "\n" + fila.exportQueueLine());
      mapEntidad.save(fila.exportMapLine());
      histEntidad.save(fila.exportHistLine());
    } catch (IOException e) {
      System.err.println("[SERVIDOR] persistencia: error al guardar estado (" + e.getMessage() + ")");
    }
  }

  private boolean hasPersistenciaEntidades() {
    return stationsEntidad != null
        && queueEntidad != null
        && mapEntidad != null
        && histEntidad != null;
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
