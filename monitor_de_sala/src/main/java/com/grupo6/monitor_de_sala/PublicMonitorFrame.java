package com.grupo6.monitor_de_sala;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class PublicMonitorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final int MONITOR_PORT = 3002;
  private static final int HISTORY_LIMIT = 5;

  private final JLabel currentTurnLabel;
  private final JLabel statusLabel;
  private final List<JLabel> historyRows = new ArrayList<>();
  private final Deque<String> callHistory = new ArrayDeque<>();
  private ServerSocket serverSocket;

  public PublicMonitorFrame() {
    setTitle("Monitor de Sala");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(720, 480);
    setLocationRelativeTo(null);

    JLabel currentTitle = new JLabel("Turno actual", SwingConstants.CENTER);
    currentTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));

    currentTurnLabel = new JLabel("-", SwingConstants.CENTER);
    currentTurnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 72));
    currentTurnLabel.setForeground(new Color(0xff, 0xff, 0xff));
    currentTurnLabel.setOpaque(true);
    currentTurnLabel.setBackground(new Color(0x15, 0x65, 0xc0));
    currentTurnLabel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

    statusLabel = new JLabel("Estado: esperando turno", SwingConstants.CENTER);

    JPanel currentPanel = new JPanel(new BorderLayout());
    currentPanel.setBorder(BorderFactory.createTitledBorder("Atencion"));
    currentPanel.add(currentTitle, BorderLayout.NORTH);
    currentPanel.add(currentTurnLabel, BorderLayout.CENTER);
    currentPanel.add(statusLabel, BorderLayout.SOUTH);

    JPanel historyPanel = new JPanel(new GridLayout(5, 1, 8, 8));
    historyPanel.setBorder(BorderFactory.createTitledBorder("Historial"));
    for (int index = 0; index < HISTORY_LIMIT; index++) {
      JLabel row = historyRow("-");
      historyRows.add(row);
      historyPanel.add(row);
    }

    setLayout(new BorderLayout(8, 8));
    add(currentPanel, BorderLayout.CENTER);
    add(historyPanel, BorderLayout.SOUTH);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (serverSocket != null && !serverSocket.isClosed()) {
          try {
            serverSocket.close();
          } catch (IOException ignored) {
            // No action needed during close.
          }
        }
      }
    });

    startServerListener();
  }

  private static JLabel historyRow(String doc) {
    JLabel row = new JLabel(doc, SwingConstants.CENTER);
    row.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
    row.setOpaque(true);
    row.setBackground(new Color(0xf5, 0xf5, 0xf5));
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xdd, 0xdd, 0xdd)),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    return row;
  }

  private void startServerListener() {
    Thread listenerThread = new Thread(() -> {
      try (ServerSocket localServerSocket = new ServerSocket(MONITOR_PORT)) {
        serverSocket = localServerSocket;
        while (!Thread.currentThread().isInterrupted()) {
          try (Socket socket = localServerSocket.accept();
               BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String dni = reader.readLine();
            if (dni == null || dni.trim().isEmpty()) {
              continue;
            }
            String normalizedDni = dni.trim();
            SwingUtilities.invokeLater(() -> updateTurn(normalizedDni));
          } catch (IOException clientError) {
            if (!localServerSocket.isClosed()) {
              SwingUtilities.invokeLater(() ->
                  statusLabel.setText("Error de red en recepcion: " + clientError.getMessage()));
            }
          }
        }
      } catch (IOException serverError) {
        SwingUtilities.invokeLater(() ->
            statusLabel.setText("No se pudo iniciar servidor: " + serverError.getMessage()));
      }
    }, "monitor-server-listener");

    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void updateTurn(String dni) {
    callHistory.addFirst(dni);
    while (callHistory.size() > HISTORY_LIMIT) {
      callHistory.removeLast();
    }

    currentTurnLabel.setText(dni);
    statusLabel.setText("Estado: Atencion " + dni);

    int index = 0;
    for (String value : callHistory) {
      historyRows.get(index).setText(value);
      index++;
    }
    while (index < HISTORY_LIMIT) {
      historyRows.get(index).setText("-");
      index++;
    }
  }
}
