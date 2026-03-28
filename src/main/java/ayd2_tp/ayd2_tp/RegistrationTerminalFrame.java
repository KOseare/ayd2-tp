package ayd2_tp.ayd2_tp;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class RegistrationTerminalFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public RegistrationTerminalFrame() {
        setTitle("Terminal de Registro");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(440, 220);
        setLocationRelativeTo(null);

        JLabel title = new JLabel("Ingrese su número de documento");
        JLabel hint = new JLabel("Ejemplo mostrado: 99.876.543");

        JTextField documentField = new JTextField("99.876.543");
        documentField.setColumns(18);

        JButton joinButton = new JButton("Unirse a la lista de espera");

        JPanel form = new JPanel(new GridLayout(4, 1, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        form.add(title);
        form.add(hint);
        form.add(documentField);
        form.add(joinButton);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
    }
}
