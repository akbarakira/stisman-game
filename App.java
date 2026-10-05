import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;  
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class App {
    public static void main(String[] args) throws Exception {
        int rowCount = 21;
        int columnCount = 19;
        int tileSize = 32;
        int boardWidth = columnCount * tileSize;
        int boardHeight = rowCount * tileSize;

        JFrame frame = new JFrame("STIS-MAN");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout cardLayout = new CardLayout();
        JPanel screens = new JPanel(cardLayout);
        screens.setPreferredSize(new Dimension(boardWidth, boardHeight));
        JPanel menuPanel = createMenuPanel(frame, screens, cardLayout);
        screens.add(menuPanel, "menu");
        frame.add(screens, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel createMenuPanel(JFrame frame, JPanel screens, CardLayout cardLayout) {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(Color.BLACK);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 40, 8, 40);

        JLabel title = new JLabel("STIS-MAN", SwingConstants.CENTER);
        title.setForeground(Color.YELLOW);
        title.setFont(new Font("Arial", Font.BOLD, 42));
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 40, 6, 40);
        menuPanel.add(title, constraints);

        JLabel subtitle = new JLabel("Selamat datang di labirin STIS", SwingConstants.CENTER);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 40, 24, 40);
        menuPanel.add(subtitle, constraints);

        JButton startButton = createMenuButton("MULAI");
        constraints.gridy = 2;
        constraints.insets = new Insets(6, 80, 6, 80);
        menuPanel.add(startButton, constraints);

        JButton helpButton = createMenuButton("PETUNJUK");
        constraints.gridy = 3;
        menuPanel.add(helpButton, constraints);

        JButton exitButton = createMenuButton("KELUAR");
        constraints.gridy = 4;
        menuPanel.add(exitButton, constraints);

        startButton.addActionListener(event -> {
            final STISMan[] gameHolder = new STISMan[1];
            gameHolder[0] = new STISMan(() -> {
                screens.remove(gameHolder[0]);
                screens.revalidate();
                screens.repaint();
                cardLayout.show(screens, "menu");
            });
            STISMan game = gameHolder[0];
            screens.add(game, "game");
            cardLayout.show(screens, "game");
            game.requestFocusInWindow();
            frame.setTitle("STIS-MAN");
        });

        helpButton.addActionListener(event -> JOptionPane.showMessageDialog(
                frame,
                "Gunakan tombol panah untuk bergerak.\n" +
                        "Kumpulkan semua makanan dan hindari hantu!",
                "Petunjuk STIS-MAN",
                JOptionPane.INFORMATION_MESSAGE));

        exitButton.addActionListener(event -> System.exit(0));
        return menuPanel;
    }

    private static JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.BLACK);
        button.setBackground(Color.YELLOW);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setPreferredSize(new Dimension(210, 48));
        return button;

    }
}
