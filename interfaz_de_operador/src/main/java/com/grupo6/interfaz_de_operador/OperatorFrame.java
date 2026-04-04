package com.grupo6.interfaz_de_operador;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import com.grupo6.ui.AppUiTheme;

public class OperatorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final int OPERATOR_PORT = Environment.OPERATOR_PORT;
  private static final String MONITOR_HOST = Environment.MONITOR_HOST;
  private static final int MONITOR_PORT = Environment.MONITOR_PORT;

  private final Queue<String> waitingQueue = new LinkedList<>();
  private final JLabel queueCountLabel;
  private final JLabel nextInQueueLabel;
  private final JLabel lastCalledCaption;
  private final JLabel lastCalledDniLabel;
  private final JLabel statusLabel;
  private ServerSocket serverSocket;

  public OperatorFrame() {
    setTitle("Puesto de Operador");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(440, 400));
    setSize(480, 420);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    Font secondaryFont = base.deriveFont(Font.PLAIN, 13f);
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);

    queueCountLabel = new JLabel("Clientes en espera: 0", SwingConstants.CENTER);
    queueCountLabel.setFont(secondaryFont);
    queueCountLabel.setForeground(AppUiTheme.TEXT_MUTED);

    nextInQueueLabel = new JLabel("Proximo en cola: -", SwingConstants.CENTER);
    nextInQueueLabel.setFont(secondaryFont);
    nextInQueueLabel.setForeground(AppUiTheme.TEXT_MUTED);

    lastCalledCaption = new JLabel("ULTIMO LLAMADO", SwingConstants.CENTER);
    lastCalledCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    lastCalledCaption.setForeground(AppUiTheme.TEXT_MUTED);

    lastCalledDniLabel = new JLabel("-", SwingConstants.CENTER);
    lastCalledDniLabel.setFont(base.deriveFont(Font.BOLD, 44f));
    lastCalledDniLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
    lastCalledDniLabel.setOpaque(false);

    statusLabel = new JLabel("Estado: esperando clientes", SwingConstants.CENTER);
    statusLabel.setFont(statusFont);
    statusLabel.setForeground(AppUiTheme.TEXT_BODY);

    JButton callNextButton = new JButton("Llamar siguiente");
    callNextButton.setFont(base.deriveFont(Font.BOLD, 14f));
    callNextButton.setMargin(new Insets(12, 28, 12, 28));
    callNextButton.addActionListener(e -> callNextClient());

    JPanel queueInfoPanel = new JPanel(new GridBagLayout());
    queueInfoPanel.setOpaque(false);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(0, 0, 4, 0);
    queueInfoPanel.add(queueCountLabel, gc);
    gc.gridy = 1;
    gc.insets = new Insets(0, 0, 0, 0);
    queueInfoPanel.add(nextInQueueLabel, gc);

    JPanel hero = new JPanel(new BorderLayout(0, 10));
    hero.setBackground(AppUiTheme.BG_HERO);
    hero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(20, 24, 28, 24)));
    hero.add(lastCalledCaption, BorderLayout.NORTH);
    hero.add(lastCalledDniLabel, BorderLayout.CENTER);

    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);
    footer.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    footer.add(statusLabel, BorderLayout.NORTH);
    JPanel buttonRow = new JPanel();
    buttonRow.setOpaque(false);
    buttonRow.add(callNextButton);
    footer.add(buttonRow, BorderLayout.CENTER);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(queueInfoPanel, BorderLayout.NORTH);
    root.add(hero, BorderLayout.CENTER);
    root.add(footer, BorderLayout.SOUTH);

    setContentPane(root);
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
          lastCalledDniLabel.setText(calledDni);
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
