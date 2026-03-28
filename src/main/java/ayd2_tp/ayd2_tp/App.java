package ayd2_tp.ayd2_tp;

public class App {

    public static void main(String[] args) {
    	App.createAndShowGui();
    }

    private static void createAndShowGui() {
        new RegistrationTerminalFrame().setVisible(true);
        new OperatorFrame().setVisible(true);
        new PublicMonitorFrame().setVisible(true);
    }
}
