package ai;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            GardenGUI gui = new GardenGUI();
            gui.setVisible(true);
        });
    }
}