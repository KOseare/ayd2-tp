package ayd2_tp.ayd2_tp;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class OperatorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public OperatorFrame() {
        setTitle("Puesto de Operador");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(420, 280);
        setLocationRelativeTo(null);

        JLabel queueNumber = new JLabel("Clientes en espera: 5", SwingConstants.CENTER);
        JLabel lastCalled = new JLabel("Último llamado: Test", SwingConstants.CENTER);

        JButton callNextButton = new JButton("Llamar siguiente");

        JPanel north = new JPanel(new GridLayout(3, 1, 6, 6));
        north.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        north.add(queueNumber);
        north.add(lastCalled);

        JPanel south = new JPanel();
        south.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        south.add(callNextButton);

        setLayout(new BorderLayout());
        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

    }
    
}
