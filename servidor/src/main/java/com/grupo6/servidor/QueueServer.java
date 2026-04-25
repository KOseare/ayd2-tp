package com.grupo6.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class QueueServer {

  private final int port;
  private final Deque<String> waitingQueue = new ArrayDeque<>();
  private final Set<String> waitingSet = new HashSet<>();
  private final Map<String, AttendingClient> activeByStation = new HashMap<>();
  private final Set<String> claimedStationIds = new HashSet<>();
  private final List<PrintWriter> monitorSubscribers = new ArrayList<>();
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  public QueueServer(int port) {
    this.port = port;
  }

  public void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Servidor iniciado en puerto " + port);
      while (true) {
        Socket socket = serverSocket.accept();
        Thread clientThread = new Thread(() -> handleClient(socket), "server-client-handler");
        clientThread.setDaemon(true);
        clientThread.start();
      }
    }
  }

  private void handleClient(Socket socket) {
    try (Socket localSocket = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(localSocket.getOutputStream(), true)) {

      String firstLine = reader.readLine();
      if (firstLine == null || firstLine.trim().isEmpty()) {
        writer.println("ERROR|EMPTY_MESSAGE");
        return;
      }

      String[] parts = firstLine.trim().split("\\|");
      String command = parts[0];

      if ("SUBSCRIBE_MONITOR".equals(command)) {
        subscribeMonitor(writer, reader);
        return;
      }

      if ("REGISTER".equals(command)) {
        handleRegister(parts, writer);
        return;
      }

      if ("CLAIM_STATION".equals(command)) {
        handleClaimStation(parts, writer);
        return;
      }

      if ("RELEASE_STATION".equals(command)) {
        handleReleaseStation(parts, writer);
        return;
      }

      if ("GET_QUEUE_SIZE".equals(command)) {
        handleGetQueueSize(writer);
        return;
      }

      if ("CALL_NEXT".equals(command)) {
        handleCallNext(parts, writer);
        return;
      }

      if ("RENOTIFY".equals(command)) {
        handleRenotify(parts, writer);
        return;
      }

      if ("FINALIZE".equals(command)) {
        handleFinalize(parts, writer);
        return;
      }

      writer.println("ERROR|UNKNOWN_COMMAND");
    } catch (IOException ignored) {
      // Client disconnected.
    }
  }

  private void subscribeMonitor(PrintWriter writer, BufferedReader reader) throws IOException {
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

  private synchronized void handleRegister(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_REGISTER");
      return;
    }
    String dni = parts[1].trim();
    if (dni.isEmpty() || !isValidDni(dni)) {
      writer.println("ERROR|INVALID_DNI");
      return;
    }
    if (waitingSet.contains(dni)) {
      writer.println("ERROR|ALREADY_IN_QUEUE");
      return;
    }
    if (isActiveDni(dni)) {
      writer.println("ERROR|ALREADY_IN_ATTENTION");
      return;
    }
    waitingQueue.offerLast(dni);
    waitingSet.add(dni);
    writer.println("OK|REGISTERED|" + dni);
  }

  private synchronized void handleClaimStation(String[] parts, PrintWriter writer) {
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
  }

  private synchronized void handleReleaseStation(String[] parts, PrintWriter writer) {
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
  }

  private synchronized void handleGetQueueSize(PrintWriter writer) {
    writer.println("OK|QUEUE_SIZE|" + waitingQueue.size());
  }

  private synchronized void handleCallNext(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    if (stationId.isEmpty()) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    AttendingClient previousClient = activeByStation.get(stationId);

    String nextDni = waitingQueue.pollFirst();
    if (nextDni == null) {
      if (previousClient != null) {
        writer.println("ERROR|NO_PENDING_KEEPING_CURRENT|" + previousClient.dni);
      } else {
        writer.println("OK|NO_PENDING");
      }
      return;
    }
    waitingSet.remove(nextDni);

    if (previousClient != null) {
      broadcastToMonitors("EVENT|REMOVED|" + previousClient.dni + "|" + stationId);
    }

    AttendingClient client = new AttendingClient(nextDni);
    activeByStation.put(stationId, client);
    writer.println("OK|CALLED|" + nextDni);

    broadcastToMonitors("EVENT|CALL|" + nextDni + "|" + stationId);
  }

  private synchronized void handleRenotify(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    AttendingClient activeClient = activeByStation.get(stationId);
    if (activeClient == null) {
      writer.println("ERROR|NO_ACTIVE_CLIENT");
      return;
    }

    activeClient.renotifyAttempts++;
    int attempts = activeClient.renotifyAttempts;
    broadcastToMonitors("EVENT|RENOTIFY|" + activeClient.dni + "|" + stationId + "|" + attempts);

    if (attempts >= 3) {
      activeByStation.remove(stationId);
      writer.println("OK|REMOVED_BY_LIMIT|" + activeClient.dni);
      broadcastToMonitors("EVENT|REMOVED|" + activeClient.dni + "|" + stationId);
      return;
    }

    writer.println("OK|RENOTIFIED|" + activeClient.dni + "|" + attempts);
  }

  private synchronized void handleFinalize(String[] parts, PrintWriter writer) {
    if (parts.length < 2) {
      writer.println("ERROR|INVALID_STATION");
      return;
    }
    String stationId = parts[1].trim();
    AttendingClient activeClient = activeByStation.remove(stationId);
    if (activeClient == null) {
      writer.println("ERROR|NO_ACTIVE_CLIENT");
      return;
    }
    writer.println("OK|FINALIZED|" + activeClient.dni);
    broadcastToMonitors("EVENT|FINALIZED|" + activeClient.dni + "|" + stationId);
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

  private boolean isActiveDni(String dni) {
    for (AttendingClient activeClient : activeByStation.values()) {
      if (activeClient.dni.equals(dni)) {
        return true;
      }
    }
    return false;
  }

  private boolean isValidDni(String dni) {
    if (!NUMERIC_PATTERN.matcher(dni).matches()) {
      return false;
    }
    return dni.length() == 7 || dni.length() == 8;
  }

  private static final class AttendingClient {
    private final String dni;
    private int renotifyAttempts;

    private AttendingClient(String dni) {
      this.dni = dni;
      this.renotifyAttempts = 0;
    }
  }
}
