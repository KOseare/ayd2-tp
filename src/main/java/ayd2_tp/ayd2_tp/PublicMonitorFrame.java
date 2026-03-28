package ayd2_tp.ayd2_tp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class PublicMonitorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

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

        JPanel historyPanel = new JPanel(new GridLayout(4, 1, 8, 8));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Últimos llamados"));
        historyPanel.add(historyRow("11.111.111"));
        historyPanel.add(historyRow("22.222.222"));
        historyPanel.add(historyRow("33.333.333"));
        historyPanel.add(historyRow("44.444.444"));

        setLayout(new BorderLayout(8, 8));
        add(currentPanel, BorderLayout.CENTER);
        add(historyPanel, BorderLayout.SOUTH);
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
}
