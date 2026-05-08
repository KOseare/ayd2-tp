package com.grupo6.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;

public class Monitor {
  private final List<ServerAddress> nodos = Environment.nodosServidores;
  private int activeNodeId = -1;

  public void start() {
    while (true) {
      if (activeNodeId < 0) {
        System.out.println("No hay un nodo activo");
        selectNewActiveNode();
      } else {
        String response = sendCommandTo(nodos.get(activeNodeId), "PING");
        System.out.println("PING response: " + response);
        if (!response.toUpperCase().contains("OK")) {
          // TODO: Lógica de reintento y buscar nuevo nodo activo
          selectNewActiveNode();
        }
      }
      sleep();
    }
  }

  private void sleep() {
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      System.out.println("InterruptedException: " + e.getMessage());
    }

  }

  private void setActiveNodeAndNotify(int id) {
    activeNodeId = id;
    System.out.println("Active node: " + id);
    for (ServerAddress addr : nodos) {
      sendCommandTo(addr, "CURRENT_ACTIVE_NODE|" + id);
    }
  }

  private void selectNewActiveNode() {
    for (ServerAddress addr : nodos) {
      String response = sendCommandTo(addr, "STATUS_UPDATE_REQUEST");
      if (response.toUpperCase().contains("STANDBY")) {
        response = sendCommandTo(addr, "START");
        if (response.toUpperCase().contains("OK")) {
          setActiveNodeAndNotify(nodos.indexOf(addr));
          break;
        }
      }
    }
    // TODO: Caso en el que ningún nodo está en estado STANDBY
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
      return "ERROR|NETWORK|" + e.getMessage();
    }
  }
}
