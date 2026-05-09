package com.grupo6.servidor;

import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  private static int port = 0;
  private static Controlador service = new Controlador();
  private static String status = "standby"; // initial, synchronizing, standby, active
  private static int id = -1;

  public static void main(String[] args) {
    try {
      id = Integer.parseInt(args[0]);
      final ServerAddress addr = Environment.nodosServidores.get(id);
      port = addr.port;
      startServer(port, service);
    } catch (Exception e) {
      System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
    }
  }

  private static void startServer(int port, Controlador service) {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Servidor iniciado en puerto " + port);
      while (true) {
        Socket socket = serverSocket.accept();
        Thread clientThread = new Thread(() -> handleEvent(socket, service), "server-client-handler");
        clientThread.setDaemon(true);
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private static void handleEvent(Socket socket, Controlador service) {
    try (Socket localSocket = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(localSocket.getOutputStream(), true)) {
      String firstLine = reader.readLine();
      System.out.println("Nueva petición: " + firstLine);
      if (firstLine == null || firstLine.trim().isEmpty()) {
        writer.println("ERROR|EMPTY_MESSAGE");
        return;
      }

      final boolean isMonitorEvent = handleMonitorEvent(writer, firstLine);
      if (!isMonitorEvent && status.equals("ACTIVE")) {
        handleClient(reader, writer, firstLine, service);
      }
    } catch (IOException ignored) {
      // Client disconnected.
    }
  }

  private static boolean handleMonitorEvent(PrintWriter writer, String message) {
    if (message.toUpperCase().contains("START")) {
      status = "ACTIVE";
      writer.println("OK");
    } else if (message.toUpperCase().contains("STATUS_UPDATE_REQUEST")) {
      writer.println(status.toUpperCase());
    } else if (message.toUpperCase().contains("UPDATE_STATE")) {
      // TODO: Update state
    } else if (message.toUpperCase().contains("PING") && status == "ACTIVE") {
      writer.println("OK");
    } else {
      return false;
    }
    return true;
  }

  private static void handleClient(BufferedReader reader, PrintWriter writer, String message, Controlador service) {
    try {
      String[] parts = message.trim().split("\\|");
      String command = parts[0];

      if ("SUBSCRIBE_MONITOR".equals(command)) {
        service.subscribeMonitor(writer, reader);
        return;
      }
      if ("SUBSCRIBE_OPERATOR".equals(command)) {
        service.subscribeOperator(writer, reader);
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
      System.out.println("Error: " + e.getMessage());
    }
  }
}
