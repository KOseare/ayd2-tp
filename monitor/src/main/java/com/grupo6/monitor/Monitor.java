package com.grupo6.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;

public class Monitor {
  private final List<ServerAddress> nodos = Environment.nodosServidores;
  private int port = Environment.monitorPort;
  private int activeNodeId = -1;

  private static void log(String message) {
    System.out.println("[MONITOR] " + message);
  }

  private static void logErr(String message) {
    System.err.println("[MONITOR] " + message);
  }

  public void start() {
    log("iniciado; nodos=" + nodos.size());
    try {
      startServer();
    } catch (IOException e) {
      logErr("No se pudo iniciar el servidor.");
      return;
    }
    while (true) {
      if (activeNodeId < 0) {
        log("sin lider - eleccion");
        selectNewActiveNode();
      } else {
        String response = sendCommandTo(nodos.get(activeNodeId), "PING");
        if (!response.toUpperCase().contains("OK")) {
          log("PING fallo al lider indice " + activeNodeId + " (" + response + ") - reeleccion");
          selectNewActiveNode();
        }
      }
      sleep();
    }
  }

  private void startServer() throws IOException {
    final Thread serverThread = new Thread(() -> {
      try (final ServerSocket serverSocket = new ServerSocket(port)) {
        log("Servidor escuchando en puerto " + port);
        while (true) {
          Socket socket = serverSocket.accept();
          Thread clientThread = new Thread(() -> handleClient(socket), "server-client-handler");
          clientThread.setDaemon(true);
          clientThread.start();
        }
      } catch (IOException e) {
        logErr("Error en ServerSocket: " + e.getMessage());
      }
    });
    serverThread.setDaemon(true);
    serverThread.start();
  }

  public void handleClient(Socket socket) {
    try (Socket localSocket = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(localSocket.getOutputStream(), true)) {
      final String message = reader.readLine();
      log("request: " + message);
      final String[] parts = message.trim().split("\\|");
      final String command = parts[0];

      if ("GET_ACTIVE_NODE".equals(command)) {
        writer.println("OK|" + activeNodeId);
        return;
      }

      writer.println("ERROR|UNKNOWN_COMMAND");

    } catch (IOException e) {
      logErr("handleClient: " + e.getMessage());
    }
  }

  private void sleep() {
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void setActiveNodeAndNotify(int id) {
    activeNodeId = id;
    log("lider=" + id + "; difundiendo CURRENT_ACTIVE_NODE");
    for (int i = 0; i < nodos.size(); i++) {
      sendCommandTo(nodos.get(i), "CURRENT_ACTIVE_NODE|" + id);
    }
  }

  private void selectNewActiveNode() {
    for (int i = 0; i < nodos.size(); i++) {
      final ServerAddress addr = nodos.get(i);
      String response = sendCommandTo(addr, "STATUS_UPDATE_REQUEST");
      if (response.toUpperCase().contains("STANDBY")) {
        response = sendCommandTo(addr, "START");
        if (response.toUpperCase().contains("OK")) {
          setActiveNodeAndNotify(i);
          return;
        }
      }
    }
    logErr("eleccion fallida: ningun STANDBY acepto START");
  }

  private String sendCommandTo(ServerAddress node, String command) {
    try {
      final Socket socket = new Socket(node.host, node.port);
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer.println(command);
      String response = reader.readLine();
      socket.close();
      if (response == null) {
        return "ERROR|NO_RESPONSE";
      }
      return response;
    } catch (IOException e) {
      logErr("error red hacia " + node.host + ":" + node.port + " - " + e.getMessage());
      return "ERROR|NETWORK|" + e.getMessage();
    }
  }
}
