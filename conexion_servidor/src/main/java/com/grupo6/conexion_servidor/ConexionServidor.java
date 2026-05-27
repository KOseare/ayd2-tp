package com.grupo6.conexion_servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;

public class ConexionServidor {
  private static final int maxTries = 3;
  private final List<ServerAddress> nodosServidores = Environment.nodosServidores;
  private Socket socket = null;
  private BufferedReader reader = null;
  private int activeId = -1;

  private boolean ensureInitialized() {
    if (activeId < 0) {
      return hallarNodoActivo();
    }
    return true;
  }

  private boolean hallarNodoActivo() {
    System.out.println("Buscando nodo activo...");
    boolean enc = false;
    int i = 0;
    while (!enc && i < nodosServidores.size()) {
      final ServerAddress nodo = nodosServidores.get(i);
      final String response = socketMsg("PING", nodo.host, nodo.port);
      System.out.println(response);
      if (response.toUpperCase().contains("OK")) {
        enc = true;
        activeId = i;
        System.out.println("Se hallo un nodo activo");
      }
      i++;
    }
    return enc;
  }

  public void subscribeAndListen(String subscribeCommand, Callback onMessage, Callback onError) {
    Thread t = new Thread(() -> {
      ensureInitialized();
      int tries = 1;
      while (!Thread.currentThread().isInterrupted()) {
        if (activeId < 0 || activeId >= nodosServidores.size()) {
          hallarNodoActivo();
          sleepQuietly(2500);
          continue;
        }
        try {
          final ServerAddress addr = nodosServidores.get(activeId);
          socket = new Socket(addr.host, addr.port);
          PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
          reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          writer.println(subscribeCommand);
          String subscribedAck = reader.readLine();
          if (subscribedAck == null || !subscribedAck.startsWith("OK|SUBSCRIBED")) {
            onError.onMessage("Error de suscripcion");
            sleepQuietly(2000);
            tries++;
            if (tries >= maxTries) {
              hallarNodoActivo();
              tries = 0;
            }
            continue;
          }
          onMessage.onMessage("OK|CONNECTED");
          String response = null;
          while ((response = reader.readLine()) != null) {
            onMessage.onMessage(response);
          }
        } catch (IOException e) {
          onError.onMessage("ERROR|NETWORK|" + e.getMessage());
          sleepQuietly(2000);
          tries++;
          if (tries >= maxTries) {
            hallarNodoActivo();
            tries = 0;
          }
        }
      }
    }, "monitor-client-listener");
    t.setDaemon(true);
    t.start();
  }

  public String sendCommand(String command) {
    final boolean initialized = ensureInitialized();
    if (!initialized) {
      return "ERROR|ACTIVE_NODE_NOT_FOUND";
    }
    String response = sendCommandOneTry(command);
    int tries = 1;
    while (isServerFault(response) && tries < maxTries) {
      response = sendCommandOneTry(command);
      tries++;
    }
    if (tries >= maxTries) {
      final boolean success = hallarNodoActivo();
      if (success) {
        return sendCommand(command);
      }
    }
    return response;
  }

  private boolean isServerFault(String response) {
    if (response.toUpperCase().contains("ERROR|NO_RESPONSE"))
      return true;
    else if (response.toUpperCase().contains("ERROR|NETWORK"))
      return true;
    return false;
  }

  private String sendCommandOneTry(String command) {
    try {
      final ServerAddress addr = nodosServidores.get(activeId);
      socket = new Socket(addr.host, addr.port);
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer.println(command);
      String response = reader.readLine();
      if (response == null) {
        return "ERROR|NO_RESPONSE";
      }
      return response;
    } catch (IOException e) {
      return "ERROR|NETWORK|" + e.getMessage();
    }
  }

  private String socketMsg(String msg, String host, int port) {
    try {
      final Socket socket = new Socket(host, port);
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer.println(msg);
      String response = reader.readLine();
      socket.close();
      if (response == null) {
        return "ERROR|NO_RESPONSE";
      }
      return response;
    } catch (IOException e) {
      return "ERROR|NETWORK|" + e.getMessage();
    }
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }
}
