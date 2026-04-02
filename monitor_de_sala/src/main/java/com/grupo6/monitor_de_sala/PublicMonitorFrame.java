package com.grupo6.monitor_de_sala;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class PublicMonitorFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  JPanel historyPanel;

  public PublicMonitorFrame() {
    setTitle("Monitor de Sala");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(720, 480);
    setLocationRelativeTo(null);

    JLabel currentLabel = new JLabel("12.345.678", SwingConstants.CENTER);
    currentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 72));
    currentLabel.setForeground(new Color(0x1a, 0x1a, 0x1a));
    currentLabel.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

    JPanel currentPanel = new JPanel(new BorderLayout());
    currentPanel.setBorder(BorderFactory.createTitledBorder("Documento en llamado"));
    currentPanel.add(currentLabel, BorderLayout.CENTER);

    historyPanel = new JPanel(new GridLayout(4, 1, 8, 8));
    historyPanel.setBorder(BorderFactory.createTitledBorder("Últimos llamados"));
    historyPanel.add(historyRow("11.111.111"));
    // historyPanel.add(historyRow("22.222.222"));
    // historyPanel.add(historyRow("33.333.333"));
    // historyPanel.add(historyRow("44.444.444"));

    setLayout(new BorderLayout(8, 8));
    add(currentPanel, BorderLayout.CENTER);
    add(historyPanel, BorderLayout.SOUTH);

    getData();
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

  public void getData() {
    new Thread() {
      public void run() {
        try {
          ServerSocket serverSocket = new ServerSocket(3001);
          while (true) {
            Socket socket = serverSocket.accept();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();

            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                historyPanel.add(historyRow(msg));
                historyPanel.revalidate(); // Recalculates the layout
                historyPanel.repaint(); // Visually redraws the panel
              }
            });

            System.out.println(msg + "\n");
            socket.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    }.start();
  }
}
