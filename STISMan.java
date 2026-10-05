import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class STISMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;
        Image normalImage;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        char requestedDirection = 0;
        int velocityX = 0;
        int velocityY = 0;
        boolean defeated = false;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.normalImage = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.requestedDirection = direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void requestDirection(char direction) {
            this.requestedDirection = direction;
        }

        boolean tryRequestedDirection() {
            if (requestedDirection == 0) {
                return false;
            }

            char previousDirection = direction;
            int previousVelocityX = velocityX;
            int previousVelocityY = velocityY;

            if (!canTurnWithinOneTile()) {
                requestedDirection = 0;
                return false;
            }

            direction = requestedDirection;
            updateVelocity();
            for (Block wall : walls) {
                if (x + velocityX < wall.x + wall.width &&
                        x + velocityX + width > wall.x &&
                        y + velocityY < wall.y + wall.height &&
                        y + velocityY + height > wall.y) {
                    direction = previousDirection;
                    velocityX = previousVelocityX;
                    velocityY = previousVelocityY;
                    return false;
                }
            }

            return true;
        }

        private boolean canTurnWithinOneTile() {
            int stepX = Integer.signum(velocityX) * tileSize / 4;
            int stepY = Integer.signum(velocityY) * tileSize / 4;
            int requestedVelocityX = 0;
            int requestedVelocityY = 0;

            if (requestedDirection == 'U') {
                requestedVelocityY = -tileSize / 4;
            }
            else if (requestedDirection == 'D') {
                requestedVelocityY = tileSize / 4;
            }
            else if (requestedDirection == 'L') {
                requestedVelocityX = -tileSize / 4;
            }
            else if (requestedDirection == 'R') {
                requestedVelocityX = tileSize / 4;
            }

            for (int distance = 0; distance <= tileSize; distance += tileSize / 4) {
                int candidateX = x + stepX * distance / (tileSize / 4);
                int candidateY = y + stepY * distance / (tileSize / 4);
                if (!hitsWall(candidateX + requestedVelocityX, candidateY + requestedVelocityY)) {
                    return true;
                }
            }

            return false;
        }

        private boolean hitsWall(int nextX, int nextY) {
            Block candidate = new Block(null, nextX, nextY, width, height);
            for (Block wall : walls) {
                if (collision(candidate, wall)) {
                    return true;
                }
            }
            return false;
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;
    private Image powerFoodImage;
    private Image powerUpImage;
    private Image scaredGhostImage;

    private Image STISManUpImage;
    private Image STISManDownImage;
    private Image STISManLeftImage;
    private Image STISManRightImage;

    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "X       bpo       X",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> powerUps;
    Block STISMan;

    Timer gameLoop;
    JButton restartButton;
    JButton nextButton;
    JButton menuButton;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean gameWon = false;
    boolean powerMode = false;
    int powerModeTicks = 0;
    private static final int POWER_UP_COUNT = 4;
    private static final int POWER_MODE_DURATION = 100;

    STISMan() {
        this(() -> {});
    }

    STISMan(Runnable menuAction) {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        setLayout(null);
        addKeyListener(this);
        setFocusable(true);

        restartButton = new JButton("RESTART");
        restartButton.setBounds(boardWidth / 2 - 90, boardHeight / 2 + 20, 180, 48);
        restartButton.setFont(new Font("Arial", Font.BOLD, 16));
        restartButton.setFocusPainted(false);
        restartButton.setVisible(false);
        restartButton.addActionListener(event -> restartGame());
        add(restartButton);

        nextButton = new JButton("NEXT");
        nextButton.setBounds(boardWidth / 2 - 90, boardHeight / 2 + 20, 180, 48);
        nextButton.setFont(new Font("Arial", Font.BOLD, 16));
        nextButton.setFocusPainted(false);
        nextButton.setVisible(false);
        nextButton.addActionListener(event -> restartGame());
        add(nextButton);

        menuButton = new JButton("MENU");
        menuButton.setBounds(boardWidth / 2 - 90, boardHeight / 2 + 76, 180, 48);
        menuButton.setFont(new Font("Arial", Font.BOLD, 16));
        menuButton.setFocusPainted(false);
        menuButton.setVisible(false);
        menuButton.addActionListener(event -> {
            gameLoop.stop();
            menuAction.run();
        });
        add(menuButton);

        //load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        powerFoodImage = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
        powerUpImage = new ImageIcon(getClass().getResource("./powerUp.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();

        STISManUpImage = new ImageIcon(getClass().getResource("./STISManUp.png")).getImage();
        STISManDownImage = new ImageIcon(getClass().getResource("./STISManDown.png")).getImage();
        STISManLeftImage = new ImageIcon(getClass().getResource("./STISManLeft.png")).getImage();
        STISManRightImage = new ImageIcon(getClass().getResource("./STISManRight.png")).getImage();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();

    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
        powerUps = new HashSet<Block>();
        powerMode = false;
        powerModeTicks = 0;

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P') { //STISMan
                    STISMan = new Block(STISManRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') { //food
                    Block food = new Block(powerFoodImage, x + 8, y + 8, 16, 16);
                    foods.add(food);
                }
            }
        }

        spawnPowerUps();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(STISMan.image, STISMan.x, STISMan.y, STISMan.width, STISMan.height, null);

        for (Block ghost : ghosts) {
            if (!ghost.defeated) {
                g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
            }
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        for (Block food : foods) {
            g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
        }
        for (Block powerUp : powerUps) {
            g.drawImage(powerUp.image, powerUp.x, powerUp.y, powerUp.width, powerUp.height, null);
        }

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver || gameWon) {
            g.setColor(new Color(0, 0, 0, 210));
            g.fillRect(0, 0, boardWidth, boardHeight);
            g.setColor(gameWon ? Color.GREEN : Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 38));
            String resultText = gameWon ? "Anda Menang" : "GAME OVER";
            int textWidth = g.getFontMetrics().stringWidth(resultText);
            g.drawString(resultText, boardWidth / 2 - textWidth / 2, boardHeight / 2 - 35);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Score: " + String.valueOf(score), boardWidth / 2 - 45, boardHeight / 2 - 5);
        }
        else {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize/2, 33);
        }
    }

    public void move() {
        updatePowerMode();
        if (STISMan.tryRequestedDirection()) {
            updateSTISManImage(STISMan.direction);
        }
        STISMan.x += STISMan.velocityX;
        STISMan.y += STISMan.velocityY;

        //check wall collisions
        for (Block wall : walls) {
            if (collision(STISMan, wall)) {
                STISMan.x -= STISMan.velocityX;
                STISMan.y -= STISMan.velocityY;
                break;
            }
        }

        //check ghost collisions
        for (Block ghost : ghosts) {
            if (ghost.defeated) {
                continue;
            }

            if (collision(ghost, STISMan)) {
                if (powerMode) {
                    ghost.defeated = true;
                    ghost.velocityX = 0;
                    ghost.velocityY = 0;
                    score += 50;
                    continue;
                }

                lives -= 1;
                if (lives == 0) {
                    gameOver = true;
                    restartButton.setVisible(true);
                    menuButton.setVisible(true);
                    return;
                }
                resetPositions();
            }

            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(STISMan, food)) {
                foodEaten = food;
                score += 10;
            }
        }
        foods.remove(foodEaten);

        if (foods.isEmpty()) {
            gameWon = true;
            nextButton.setVisible(true);
            menuButton.setVisible(true);
            return;
        }

        Block powerUpCollected = null;
        for (Block powerUp : powerUps) {
            if (collision(STISMan, powerUp)) {
                powerUpCollected = powerUp;
                break;
            }
        }

        if (powerUpCollected != null) {
            powerUps.remove(powerUpCollected);
            powerMode = true;
            powerModeTicks = POWER_MODE_DURATION;
            for (Block ghost : ghosts) {
                ghost.image = scaredGhostImage;
            }
        }
    }

    private void spawnPowerUps() {
        while (powerUps.size() < POWER_UP_COUNT) {
            int row = random.nextInt(rowCount);
            int column = random.nextInt(columnCount);
            if (tileMap[row].charAt(column) != ' ') {
                continue;
            }

            int powerUpX = column * tileSize + 8;
            int powerUpY = row * tileSize + 8;
            boolean locationTaken = false;
            for (Block powerUp : powerUps) {
                if (powerUp.x == powerUpX && powerUp.y == powerUpY) {
                    locationTaken = true;
                    break;
                }
            }

            if (!locationTaken) {
                Block powerUp = new Block(powerUpImage, powerUpX, powerUpY, 16, 16);
                powerUps.add(powerUp);
                foods.removeIf(food -> food.x == powerUpX && food.y == powerUpY);
            }
        }
    }

    private void updatePowerMode() {
        if (!powerMode) {
            return;
        }

        powerModeTicks--;
        if (powerModeTicks <= 0) {
            powerMode = false;
            for (Block ghost : ghosts) {
                ghost.defeated = false;
                ghost.image = ghost.normalImage;
                ghost.updateDirection(directions[random.nextInt(4)]);
            }
        }
    }

    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        STISMan.reset();
        STISMan.velocityX = 0;
        STISMan.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    public void restartGame() {
        loadMap();
        resetPositions();
        lives = 3;
        score = 0;
        gameOver = false;
        gameWon = false;
        restartButton.setVisible(false);
        nextButton.setVisible(false);
        menuButton.setVisible(false);
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver || gameWon) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) {
            return;
        }

        char requestedDirection = 0;
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            requestedDirection = 'U';
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            requestedDirection = 'D';
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            requestedDirection = 'L';
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            requestedDirection = 'R';
        }

        if (requestedDirection != 0) {
            STISMan.requestDirection(requestedDirection);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver || gameWon) {
            return;
        }
    }

    private void updateSTISManImage(char direction) {
        if (direction == 'U') {
            STISMan.image = STISManUpImage;
        }
        else if (direction == 'D') {
            STISMan.image = STISManDownImage;
        }
        else if (direction == 'L') {
            STISMan.image = STISManLeftImage;
        }
        else if (direction == 'R') {
            STISMan.image = STISManRightImage;
        }
    }
}
