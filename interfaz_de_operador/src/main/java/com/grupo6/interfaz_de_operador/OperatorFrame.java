package com.grupo6.interfaz_de_operador;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.grupo6.environment.Environment;

public class OperatorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final int OPERATOR_PORT = Environment.OPERATOR_PORT;
  private static final String MONITOR_HOST = Environment.MONITOR_HOST;
  private static final int MONITOR_PORT = Environment.MONITOR_PORT;

  private final Queue<String> waitingQueue = new LinkedList<>();
  private final JLabel queueCountLabel;
  private final JLabel nextInQueueLabel;
  private final JLabel lastCalledLabel;
  private final JLabel statusLabel;
  private ServerSocket serverSocket;

  public OperatorFrame() {
    setTitle("Puesto de Operador");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(420, 280);
    setLocationRelativeTo(null);

    queueCountLabel = new JLabel("Clientes en espera: 0", SwingConstants.CENTER);
    nextInQueueLabel = new JLabel("Proximo en cola: -", SwingConstants.CENTER);
    lastCalledLabel = new JLabel("Ultimo llamado: -", SwingConstants.CENTER);
    statusLabel = new JLabel("Estado: esperando clientes", SwingConstants.CENTER);

    JButton callNextButton = new JButton("Llamar Siguiente");
    callNextButton.addActionListener(e -> callNextClient());

    JPanel north = new JPanel(new GridLayout(4, 1, 6, 6));
    north.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    north.add(queueCountLabel);
    north.add(nextInQueueLabel);
    north.add(lastCalledLabel);
    north.add(statusLabel);

    JPanel south = new JPanel();
    south.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
    south.add(callNextButton);

    setLayout(new BorderLayout());
    add(north, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (serverSocket != null && !serverSocket.isClosed()) {
          try {
            serverSocket.close();
          } catch (IOException ignored) {
            // No-op: app is closing.
          }
        }
      }
    });

    startSocketListener();
  }

  private synchronized void enqueueDni(String dni) {
    waitingQueue.offer(dni);
    updateQueueLabels();
  }

  private void callNextClient() {
    String calledDni;
    synchronized (this) {
      calledDni = waitingQueue.poll();
    }
    if (calledDni == null) {
      statusLabel.setText("Estado: no hay clientes en espera.");
      return;
    }

    String dniToSend = calledDni;
    Thread sendThread = new Thread(() -> sendToMonitor(dniToSend), "operator-monitor-sender");
    sendThread.setDaemon(true);
    sendThread.start();
  }

  private void sendToMonitor(String calledDni) {
    try (Socket socket = new Socket(MONITOR_HOST, MONITOR_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
      writer.println(calledDni);
      SwingUtilities.invokeLater(() -> {
        synchronized (OperatorFrame.this) {
          lastCalledLabel.setText("Ultimo llamado: " + calledDni);
          updateQueueLabels();
        }
        statusLabel.setText("Estado: Atencion " + calledDni);
      });
    } catch (IOException e) {
      SwingUtilities.invokeLater(() -> {
        synchronized (OperatorFrame.this) {
          waitingQueue.offer(calledDni);
          updateQueueLabels();
        }
        statusLabel.setText("Error de red al enviar al monitor: " + e.getMessage());
      });
    }
  }

  private synchronized void updateQueueLabels() {
    queueCountLabel.setText("Clientes en espera: " + waitingQueue.size());
    String nextDni = waitingQueue.peek();
    nextInQueueLabel.setText("Proximo en cola: " + (nextDni == null ? "-" : nextDni));
  }

  private void startSocketListener() {
    Thread socketThread = new Thread(() -> {
      try (ServerSocket localServerSocket = new ServerSocket(OPERATOR_PORT)) {
        serverSocket = localServerSocket;
        while (!Thread.currentThread().isInterrupted()) {
          try (Socket clientSocket = localServerSocket.accept();
              BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String receivedDni = reader.readLine();
            if (receivedDni == null || receivedDni.trim().isEmpty()) {
              continue;
            }

            String normalizedDni = receivedDni.trim();
            SwingUtilities.invokeLater(() -> {
              enqueueDni(normalizedDni);
              statusLabel.setText("Estado: nuevo cliente en espera (" + normalizedDni + ")");
            });
          } catch (IOException clientError) {
            if (!localServerSocket.isClosed()) {
              SwingUtilities.invokeLater(
                  () -> statusLabel.setText("Error de red al recibir cliente: " + clientError.getMessage()));
            }
          }
        }
      } catch (IOException serverError) {
        SwingUtilities
            .invokeLater(() -> statusLabel.setText("No se pudo iniciar el servidor: " + serverError.getMessage()));
      }
    }, "operator-socket-listener");

    socketThread.setDaemon(true);
    socketThread.start();
  }

}
