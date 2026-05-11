package com.grupo6.servidor;

import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Main {
  private static int port = 0;
  private static final Controlador service = new Controlador();
  private static String status = "standby"; // "standby" o "ACTIVE"
  private static int id = -1;

  private static final Object replicaLock = new Object();
  private static volatile Thread replicaWorker = null;

  /**
   * Prefix {@code [SERVIDOR-<id>-ACTIVO|STANDBY]} for stdout logs from
   * {@link #logSrv}.
   */
  static String serverLogPrefix() {
    final String role = "ACTIVE".equalsIgnoreCase(status) ? "ACTIVO" : "STANDBY";
    final String nid = id >= 0 ? String.valueOf(id) : "?";
    return "[SERVIDOR-" + nid + "-" + role + "]";
  }

  static void logSrv(String message) {
    System.out.println(serverLogPrefix() + " " + message);
  }

  static void logSrvErr(String message) {
    System.err.println(serverLogPrefix() + " " + message);
  }

  public static void main(String[] args) {
    final Thread initialRequestThread = new Thread(() -> initialActiveNodeRequest());
    initialRequestThread.setDaemon(true);
    initialRequestThread.start();
    try {
      id = Integer.parseInt(args[0]);
      final ServerAddress addr = Environment.nodosServidores.get(id);
      port = addr.port;
      startServer(port, service);
    } catch (Exception e) {
      final String pre = id >= 0 ? serverLogPrefix() : "[SERVIDOR-?-STANDBY]";
      System.err.println(pre + " No se pudo iniciar el servidor: " + e.getMessage());
    }
  }

  private static void initialActiveNodeRequest() {
    int activeNodeId = -2;
    while (activeNodeId == -2) {
      try {
        final int activeNodeResponse = getActiveNodeId();
        if (activeNodeResponse < 0) {
          activeNodeId = -1;
          continue;
        }
        activeNodeId = activeNodeResponse;
      } catch (Exception e) {
        logSrvErr("No se pudo obtener el ID del nodo activo: " + e.getMessage());
        sleepQuietly(750);
      }
    }
    logSrv("Se obtuvo el ID del nodo activo: " + activeNodeId);
    onCurrentActiveNodeAnnouncement(activeNodeId);
  }

  private static int getActiveNodeId() throws Exception {
    final Socket socket = new Socket(Environment.monitorHost, Environment.monitorPort);
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    writer.println("GET_ACTIVE_NODE");
    final String response = reader.readLine();
    socket.close();
    if (!response.startsWith("OK")) {
      throw new Exception(response);
    }
    final String[] parts = response.split("\\|");
    final int activeNodeIdResponse = Integer.parseInt(parts[1]);
    return activeNodeIdResponse;
  }

  private static void startServer(int port, Controlador service) {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      logSrv("Servidor escuchando en puerto " + port);
      while (true) {
        Socket socket = serverSocket.accept();
        Thread clientThread = new Thread(() -> handleEvent(socket, service), "server-client-handler");
        clientThread.setDaemon(true);
        clientThread.start();
      }
    } catch (IOException e) {
      logSrv("Error en ServerSocket: " + e.getMessage());
    }
  }

  private static void handleEvent(Socket socket, Controlador service) {
    try (Socket localSocket = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(localSocket.getOutputStream(), true)) {
      String firstLine = reader.readLine();
      logSrv("request: " + firstLine);
      if (firstLine == null || firstLine.trim().isEmpty()) {
        writer.println("ERROR|EMPTY_MESSAGE");
        return;
      }

      final boolean isMonitorEvent = handleMonitorEvent(writer, firstLine);
      if (!isMonitorEvent && "ACTIVE".equalsIgnoreCase(status)) {
        handleClient(reader, writer, firstLine, service);
      }
    } catch (IOException ignored) {
      // Client disconnected.
    }
  }

  private static boolean handleMonitorEvent(PrintWriter writer, String message) {
    final String trimmed = message.trim();
    final String upper = trimmed.toUpperCase();
    if (upper.startsWith("CURRENT_ACTIVE_NODE|")) {
      final String[] p = trimmed.split("\\|", -1);
      if (p.length < 2) {
        writer.println("ERROR|INVALID_MESSAGE");
        return true;
      }
      try {
        final int leader = Integer.parseInt(p[1].trim());
        onCurrentActiveNodeAnnouncement(leader);
        writer.println("OK");
      } catch (NumberFormatException e) {
        writer.println("ERROR|INVALID_LEADER");
        logSrvErr("monitor: CURRENT_ACTIVE_NODE invalido");
      }
      return true;
    }
    if (upper.contains("START")) {
      logSrv("evento monitor: START - nodo promovido a ACTIVO");
      status = "ACTIVE";
      stopReplicaClientSession();
      service.clearReplicaSubscribers();
      writer.println("OK");
      return true;
    }
    if (upper.contains("STATUS_UPDATE_REQUEST")) {
      writer.println(status.toUpperCase());
      return true;
    }
    if (upper.contains("PING") && "ACTIVE".equalsIgnoreCase(status)) {
      writer.println("OK");
      return true;
    }
    return false;
  }

  private static void onCurrentActiveNodeAnnouncement(int leaderId) {
    if (leaderId == id) {
      logSrv("cluster: confirmado como lider indice " + id);
      stopReplicaClientSession();
      return;
    }
    if (leaderId < 0 || leaderId >= Environment.nodosServidores.size()) {
      logSrvErr("CURRENT_ACTIVE_NODE: indice de lider invalido: " + leaderId);
      return;
    }
    status = "standby";
    logSrv("rol: pasivo - replica hacia lider " + leaderId);
    startReplicaClientSessionIfNeeded(leaderId);
  }

  private static void startReplicaClientSessionIfNeeded(int leaderId) {
    synchronized (replicaLock) {
      stopReplicaClientSessionUnsynchronized();
      Thread t = new Thread(() -> replicaRunLoop(leaderId), "replica-client-" + leaderId);
      t.setDaemon(true);
      replicaWorker = t;
      t.start();
    }
  }

  private static void stopReplicaClientSession() {
    synchronized (replicaLock) {
      stopReplicaClientSessionUnsynchronized();
    }
  }

  private static void stopReplicaClientSessionUnsynchronized() {
    Thread t = replicaWorker;
    replicaWorker = null;
    if (t != null) {
      t.interrupt();
      try {
        t.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void replicaRunLoop(int leaderId) {
    while (!Thread.currentThread().isInterrupted()) {
      if (leaderId == id) {
        return;
      }
      try {
        runOneReplicaConnection(leaderId);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
      sleepQuietly(2000);
    }
  }

  private static void runOneReplicaConnection(int leaderId) throws InterruptedException {
    final ServerAddress addr = Environment.nodosServidores.get(leaderId);
    try (Socket socket = new Socket(addr.host, addr.port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      socket.setSoTimeout(5000);
      out.println("SUBSCRIBE_REPLICA");
      final String ack = in.readLine();
      if (ack == null || !ack.startsWith("OK|SUBSCRIBED")) {
        logSrvErr("replica: fallo suscripcion al lider " + leaderId + " ACK=" + ack);
        return;
      }
      String line;
      while (!Thread.currentThread().isInterrupted()) {
        try {
          line = in.readLine();
        } catch (SocketTimeoutException e) {
          if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
          }
          continue;
        }
        if (line == null) {
          return;
        }
        if (line.startsWith("STATE_FULL|")) {
          try {
            synchronized (service) {
              service.applyFullStateFromLeaderLine(line);
            }
          } catch (RuntimeException ex) {
            logSrvErr("replica: error aplicando STATE_FULL (" + ex.getMessage() + ")");
          }
        }
      }
    } catch (IOException e) {
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }
      logSrvErr("replica: error de IO: " + e.getMessage());
    }
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void handleClient(BufferedReader reader, PrintWriter writer, String message, Controlador service) {
    try {
      final String[] parts = message.trim().split("\\|");
      final String command = parts[0];

      if ("SUBSCRIBE_MONITOR".equals(command)) {
        service.subscribeMonitor(writer, reader);
        return;
      }
      if ("SUBSCRIBE_OPERATOR".equals(command)) {
        service.subscribeOperator(writer, reader);
        return;
      }
      if ("SUBSCRIBE_REPLICA".equals(command)) {
        service.subscribeReplica(writer, reader);
        return;
      }

      if ("REGISTER".equals(command)) {
        service.handleRegister(parts, writer);
        return;
      }

      if ("CLAIM_STATION".equals(command)) {
        service.handleClaimStation(parts, writer);
        return;
      }

      if ("RELEASE_STATION".equals(command)) {
        service.handleReleaseStation(parts, writer);
        return;
      }

      if ("GET_QUEUE_SIZE".equals(command)) {
        service.handleGetQueueSize(writer);
        return;
      }

      if ("CALL_NEXT".equals(command)) {
        service.handleCallNext(parts, writer);
        return;
      }

      if ("RENOTIFY".equals(command)) {
        service.handleRenotify(parts, writer);
        return;
      }

      if ("FINALIZE".equals(command)) {
        service.handleFinalize(parts, writer);
        return;
      }

      writer.println("ERROR|UNKNOWN_COMMAND");

    } catch (IOException e) {
      logSrvErr("handleClient: " + e.getMessage());
    }
  }
}
