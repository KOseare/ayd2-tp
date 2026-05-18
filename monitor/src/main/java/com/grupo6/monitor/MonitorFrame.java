package com.grupo6.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import com.grupo6.ui.AppUiTheme;

public class MonitorFrame extends JFrame implements IVista {
  private static final long serialVersionUID = 1L;
  private final JLabel leaderLabel;
  private final JPanel nodesContainer;
  private final Font baseFont;

  public MonitorFrame() {
    setTitle("Monitor de Infraestructura");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(520, 450));
    setSize(560, 500);
    setLocationRelativeTo(null);

    baseFont = AppUiTheme.baseUiFont();

    JLabel titleLabel = new JLabel("ESTADO DE LOS SERVIDORES", SwingConstants.CENTER);
    titleLabel.setFont(baseFont.deriveFont(Font.BOLD, 12f));
    titleLabel.setForeground(AppUiTheme.TEXT_MUTED);

    leaderLabel = new JLabel("Servidor Principal: Buscando lider...", SwingConstants.CENTER);
    leaderLabel.setFont(baseFont.deriveFont(Font.BOLD, 20f));
    leaderLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);

    JPanel heroPanel = new JPanel(new BorderLayout(0, 8));
    heroPanel.setBackground(AppUiTheme.BG_HERO);
    heroPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(16, 20, 16, 20)));
    heroPanel.add(titleLabel, BorderLayout.NORTH);
    heroPanel.add(leaderLabel, BorderLayout.CENTER);

    nodesContainer = new JPanel();
    nodesContainer.setBackground(AppUiTheme.BG_APP);

    JScrollPane scrollPane = new JScrollPane(nodesContainer);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    root.add(heroPanel, BorderLayout.NORTH);
    root.add(scrollPane, BorderLayout.CENTER);

    setContentPane(root);
  }

  @Override
  public void setControlador(Controlador controlador) {}

  @Override
  public void actualizar(ModeloVista modelo) {
    if (modelo.activeNodeId >= 0) {
      leaderLabel.setText("Servidor Principal Activo: Nodo " + modelo.activeNodeId);
    } else {
      leaderLabel.setText("Servidor Principal: SIN LIDER (Eleccion en progreso)");
    }

    nodesContainer.removeAll();
    int totalNodes = modelo.nombresNodos.size();
    nodesContainer.setLayout(new GridLayout(Math.max(totalNodes, 5), 1, 0, 8));

    for (int i = 0; i < totalNodes; i++) {
      String name = modelo.nombresNodos.get(i);
      String state = modelo.estadosNodos.get(i);

      JPanel row = new JPanel(new BorderLayout(12, 0));
      row.setBackground(Color.WHITE);
      row.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(230, 235, 245), 1),
          BorderFactory.createEmptyBorder(12, 16, 12, 16)));

      JLabel nameLabel = new JLabel(name);
      nameLabel.setFont(baseFont.deriveFont(Font.BOLD, 14f));
      nameLabel.setForeground(new Color(40, 50, 70));

      JLabel badge = new JLabel(state, SwingConstants.CENTER);
      badge.setFont(baseFont.deriveFont(Font.BOLD, 11f));
      badge.setOpaque(true);
      badge.setPreferredSize(new Dimension(100, 26));

      if ("ACTIVE".equals(state)) {
        badge.setBackground(new Color(230, 247, 235));
        badge.setForeground(new Color(30, 130, 60));
        badge.setBorder(BorderFactory.createLineBorder(new Color(180, 230, 195), 1));
      } else if ("STANDBY".equals(state)) {
        badge.setBackground(new Color(255, 248, 225));
        badge.setForeground(new Color(190, 110, 0));
        badge.setBorder(BorderFactory.createLineBorder(new Color(255, 225, 150), 1));
      } else {
        badge.setBackground(new Color(253, 237, 237));
        badge.setForeground(new Color(190, 30, 30));
        badge.setBorder(BorderFactory.createLineBorder(new Color(245, 190, 190), 1));
      }

      row.add(nameLabel, BorderLayout.CENTER);
      row.add(badge, BorderLayout.EAST);
      nodesContainer.add(row);
    }

    nodesContainer.revalidate();
    nodesContainer.repaint();
  }
}