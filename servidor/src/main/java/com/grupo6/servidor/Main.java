package com.grupo6.servidor;

import java.io.IOException;

import com.grupo6.environment.Environment;
import com.grupo6.servidor.model.QueueModelMock;
import com.grupo6.servidor.net.QueueSocketServer;

public class Main {
  public static void main(String[] args) {
    int port = Environment.QUEUE_PORT;
    if (args != null && args.length >= 1) {
      port = Integer.parseInt(args[0]);
    }
    QueueModelMock model = new QueueModelMock();
    try {
      new QueueSocketServer(model, port).start();
    } catch (IOException e) {
      System.err.println("Server failed: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }
}
