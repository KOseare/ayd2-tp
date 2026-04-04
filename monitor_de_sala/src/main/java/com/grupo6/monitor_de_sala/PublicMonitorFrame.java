package com.grupo6.monitor_de_sala;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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

import com.grupo6.environment.Environment;
import com.grupo6.ui.AppUiTheme;

public class PublicMonitorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final int MONITOR_PORT = Environment.MONITOR_PORT;
  private static final int HISTORY_LIMIT = 5;

  private final JLabel currentTurnLabel;
  private final JLabel statusLabel;
  private final List<JLabel> historyRows = new ArrayList<>();
  private final Deque<String> callHistory = new ArrayDeque<>();
  private ServerSocket serverSocket;

  public PublicMonitorFrame() {
    setTitle("Monitor de Sala");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(520, 520));
    setSize(720, 560);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    Font historyFont = base.deriveFont(Font.PLAIN, 20f);
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);

    JLabel currentCaption = new JLabel("TURNO ACTUAL", SwingConstants.CENTER);
    currentCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    currentCaption.setForeground(AppUiTheme.TEXT_MUTED);

    currentTurnLabel = new JLabel("-", SwingConstants.CENTER);
    currentTurnLabel.setFont(base.deriveFont(Font.BOLD, 64f));
    currentTurnLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
    currentTurnLabel.setOpaque(false);

    JPanel hero = new JPanel(new BorderLayout(0, 10));
    hero.setBackground(AppUiTheme.BG_HERO);
    hero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(20, 24, 28, 24)));
    hero.add(currentCaption, BorderLayout.NORTH);
    hero.add(currentTurnLabel, BorderLayout.CENTER);

    statusLabel = new JLabel("Estado: esperando turno", SwingConstants.CENTER);
    statusLabel.setFont(statusFont);
    statusLabel.setForeground(AppUiTheme.TEXT_BODY);

    JLabel historyCaption = new JLabel("HISTORIAL RECIENTE", SwingConstants.CENTER);
    historyCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    historyCaption.setForeground(AppUiTheme.TEXT_MUTED);

    JPanel historyPanel = new JPanel(new GridLayout(HISTORY_LIMIT, 1, 8, 8));
    historyPanel.setOpaque(false);
    for (int index = 0; index < HISTORY_LIMIT; index++) {
      JLabel row = historyRow("-", historyFont);
      historyRows.add(row);
      historyPanel.add(row);
    }

    JPanel historyBlock = new JPanel(new BorderLayout(0, 10));
    historyBlock.setOpaque(false);
    historyBlock.add(historyCaption, BorderLayout.NORTH);
    historyBlock.add(historyPanel, BorderLayout.CENTER);

    JPanel statusWrap = new JPanel(new GridBagLayout());
    statusWrap.setOpaque(false);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(0, 0, 8, 0);
    statusWrap.add(statusLabel, gc);

    JPanel south = new JPanel(new BorderLayout(0, 16));
    south.setOpaque(false);
    south.add(statusWrap, BorderLayout.NORTH);
    south.add(historyBlock, BorderLayout.CENTER);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(hero, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);

    setContentPane(root);
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

  private static JLabel historyRow(String doc, Font font) {
    JLabel row = new JLabel(doc, SwingConstants.CENTER);
    row.setFont(font);
    row.setForeground(AppUiTheme.TEXT_BODY);
    row.setOpaque(true);
    row.setBackground(AppUiTheme.BG_CARD);
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_CARD, 1),
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
              SwingUtilities
                  .invokeLater(() -> statusLabel.setText("Error de red en recepcion: " + clientError.getMessage()));
            }
          }
        }
      } catch (IOException serverError) {
        SwingUtilities
            .invokeLater(() -> statusLabel.setText("No se pudo iniciar servidor: " + serverError.getMessage()));
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
