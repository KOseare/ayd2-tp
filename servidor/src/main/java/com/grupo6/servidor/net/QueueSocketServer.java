package com.grupo6.servidor.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.grupo6.environment.QueueProtocol;
import com.grupo6.servidor.model.QueueModelMock;

public class QueueSocketServer {

  private final QueueModelMock model;
  private final int port;

  public QueueSocketServer(QueueModelMock model, int port) {
    this.model = model;
    this.port = port;
  }

  public void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Queue control listening on " + port);
      while (true) {
        Socket client = serverSocket.accept();
        System.out.println("[servidor] accepted connection from " + client.getRemoteSocketAddress());
        Thread t = new Thread(() -> handleClient(client), "queue-client");
        t.setDaemon(true);
        t.start();
      }
    }
  }

  void handleClient(Socket clientSocket) {
    String remote = clientSocket.getRemoteSocketAddress().toString();
    try (Socket socket = clientSocket;
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
      String command = in.readLine();
      if (command == null) {
        System.out.println("[" + remote + "] disconnected before sending a command");
        return;
      }
      String trimmed = command.trim();
      System.out.println("[" + remote + "] received command: " + trimmed);

      // --- Routing of commands to the model ---
      if (QueueProtocol.CMD_LLAMAR_CLIENTE.equals(trimmed)) {
        String body = model.llamarCliente();
        String line = QueueProtocol.responseOk(body);
        out.println(line);
        System.out.println("[" + remote + "] " + QueueProtocol.CMD_LLAMAR_CLIENTE + " response: " + line);
        return;
      }
      if (QueueProtocol.CMD_REGISTRAR_CLIENTE.equals(trimmed)) {
        String dniLine = in.readLine();
        String dni = dniLine == null ? "" : dniLine.trim();
        System.out.println("[" + remote + "] " + QueueProtocol.CMD_REGISTRAR_CLIENTE + " received dni: " + dni);
        String body = model.registrarCliente(dni);
        String line = QueueProtocol.responseOk(body);
        out.println(line);
        System.out.println("[" + remote + "] " + QueueProtocol.CMD_REGISTRAR_CLIENTE + " response: " + line);
        return;
      }

      String line = QueueProtocol.responseErr("Unknown command: " + trimmed);
      out.println(line);
      System.out.println("[" + remote + "] response: " + line);
    } catch (IOException e) {
      System.err.println("[" + remote + "] handler error: " + e.getMessage());
    }
  }
}
