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
        System.out.println("Se halló un nodo activo");
      }
      i++;
    }
    return enc;
  }

  public String readNext() throws IOException {
    ensureInitialized();
    final ServerAddress addr = nodosServidores.get(activeId);
    final Socket socket = new Socket(addr.host, addr.port);
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    final String result = reader.readLine();
    return result;
  }

  public String sendCommand(String command) {
    ensureInitialized();
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
      Socket socket = new Socket(addr.host, addr.port);
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
}
