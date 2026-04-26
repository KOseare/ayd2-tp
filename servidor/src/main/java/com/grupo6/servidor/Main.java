package com.grupo6.servidor;

import com.grupo6.environment.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  public static void main(String[] args) {
    try {
      int port = Environment.SERVER_PORT;
      Controlador service = new Controlador();
      startServer(port, service);
    } catch (Exception e) {
      System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
    }
  }

  private static void startServer(int port, Controlador service) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Servidor iniciado en puerto " + port);
      while (true) {
        Socket socket = serverSocket.accept();
        Thread clientThread =
            new Thread(() -> handleClient(socket, service), "server-client-handler");
        clientThread.setDaemon(true);
        clientThread.start();
      }
    }
  }

  private static void handleClient(Socket socket, Controlador service) {
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
        service.subscribeMonitor(writer, reader);
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
    } catch (IOException ignored) {
      // Client disconnected.
    }
  }
}
