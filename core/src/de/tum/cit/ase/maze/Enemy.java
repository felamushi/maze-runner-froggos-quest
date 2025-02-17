package de.tum.cit.ase.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;

public class Enemy extends MazeElement implements Movable {
    private EnemyState currentState; // Current state of the enemy in the FSM.
    private static final int TILE_SIZE = 16; // each tile is 16x16
    private Direction currentDirection; // current direction
    private Character player;
    private Maze maze;
    private Animation<TextureRegion>[] animations; // Animations for different directions
    private float stateTime; // Time since the animation started
    private AStar pathfinder;
    private List<Node> currentPath;
    private int pathIndex;
    private final float REACHED_NODE_TOLERANCE = 2.0f;
    private float speed = 40.0f;

    private Animation<TextureRegion> deathAnimation;
    private boolean isDead = false;
    private boolean deathAnimationPlayed = false;
    private float deathAnimationTime = 0;

    /**
     * Constructs an enemy with specified parameters.
     *
     * @param texture        The texture for the enemy.
     * @param x              The initial x-coordinate of the enemy.
     * @param y              The initial y-coordinate of the enemy.
     * @param player         The player character to chase.
     * @param maze           The maze in which the enemy is located.
     * @param animations     Array of animations for the enemy's movement.
     * @param deathAnimation Animation to play upon the enemy's death.
     */
    public Enemy(TextureRegion texture, int x, int y, Character player, Maze maze, Animation<TextureRegion>[] animations, Animation<TextureRegion> deathAnimation) {
        super(texture, x, y, TILE_SIZE, TILE_SIZE);
        this.currentState = EnemyState.PATROLLING;
        this.currentDirection = Direction.values()[random.nextInt(Direction.values().length)]; // Random initial direction
        this.player = player; // Reference to the player character
        this.maze = maze; // Reference to the maze
        this.animations = animations;
        this.stateTime = 0f;
        this.pathfinder = new AStar(convertToNodes(maze.getLayout()));
        this.deathAnimation = deathAnimation;


    }

    /**
     * Moves the enemy in a specified direction.
     *
     * @param direction The direction in which to move the enemy.
     * @param maze      The maze to check for valid moves.
     * @param delta     The time passed since the last frame.
     */
    @Override
    public void move(Direction direction, Maze maze, float delta) {
        float speed = TILE_SIZE * delta;
        float newX = x;
        float newY = y;


        // Calculate new position
        switch (direction) {
            case UP:
                newY += speed;
                break;
            case DOWN:
                newY -= speed;
                break;
            case LEFT:
                newX -= speed;
                break;
            case RIGHT:
                newX += speed;
                break;
            default:
                return; // Invalid direction

        }

        // Collision checking
        Rectangle tempBounds = new Rectangle(bounds);
        tempBounds.setPosition(newX, newY);
        int collisionType = maze.checkCollision(tempBounds, false);

// Check for collisions with walls and doors
        if (collisionType == 0) {
            handleWallCollision();
        } else {
            maze.setElementAt((int) x / TILE_SIZE, (int) y / TILE_SIZE, -1);
            setPosition(newX, newY);
            maze.setElementAt((int) x / TILE_SIZE, (int) y / TILE_SIZE, 4);
        }
        if (!isCollisionWithWall(newX, newY, maze, currentDirection)) {
            setPosition(newX, newY);
        } else {
            handleWallCollision();
            chase(delta);
        }


    }

    /**
     * Sets the player character for the enemy to chase.
     *
     * @param player The player character.
     * @throws IllegalArgumentException If the player character is null.
     */

    public void setPlayer(Character player) {
        if (player == null) {
            throw new IllegalArgumentException("Player character cannot be null");
        }
        this.player = player;
    }

    /**
     * Converts the maze layout to a grid of nodes for pathfinding.
     *
     * @param layout The layout of the maze.
     * @return A grid of nodes representing the maze.
     */
    private Node[][] convertToNodes(int[][] layout) {
        Node[][] nodes = new Node[layout.length][layout[0].length];

        for (int x = 0; x < layout.length; x++) {
            for (int y = 0; y < layout[x].length; y++) {
                nodes[x][y] = new Node(x, y, layout[x][y] == -1);
            }
        }

        return nodes;
    }

    /**
     * Handles the enemy's patrolling behavior within the maze.
     *
     * @param delta The time passed since the last frame.
     * @param maze  The maze in which the enemy patrols.
     */
    private void patrol(float delta, Maze maze) {
        float speed = TILE_SIZE * delta;
        float projectedX = x, projectedY = y;

        switch (currentDirection) {
            case UP:
                projectedY += speed;
                break;
            case DOWN:
                projectedY -= speed;
                break;
            case LEFT:
                projectedX -= speed;
                break;
            case RIGHT:
                projectedX += speed;
                break;
        }

        // Check if the projected position collides with a wall or trap
        if (!isCollisionWithWall(projectedX, projectedY, maze, currentDirection)) {
            // If there is no collision, update the position
            setPosition(projectedX, projectedY);
        } else {
            // If there is a collision, choose a new direction
            chooseNewDirection(maze);
        }


    }

    /**
     * Determines if a collision with a wall occurs at a specified position and direction.
     *
     * @param x         The x-coordinate of the position to check.
     * @param y         The y-coordinate of the position to check.
     * @param maze      The maze containing the walls.
     * @param direction The direction of movement.
     * @return true if a collision with a wall occurs, false otherwise.
     */
    private boolean isCollisionWithWall(float x, float y, Maze maze, Direction direction) {
        int gridX = (int) (x / TILE_SIZE);
        int gridY = (int) (y / TILE_SIZE);

        // Determine the next tile based on the direction of movement
        switch (direction) {
            case UP:
                gridY += 1;
                break;
            case DOWN:
                gridY -= 1;
                break;
            case LEFT:
                gridX -= 1;
                break;
            case RIGHT:
                gridX += 1;
                break;
        }

        // Check bounds
        if (gridX < 0 || gridY < 0 || gridX >= maze.getLayout().length || gridY >= maze.getLayout()[0].length) {
            return true; // Collision with a wall (out of bounds)
        }

        int tileType = maze.getElementAt(gridX, gridY);
        return tileType == 0; // Collision with a wall if the tile type is 0
    }

    /**
     * Initiates the enemy's chasing behavior towards the player character.
     *
     * @param delta The time passed since the last frame.
     */
    private void chase(float delta) {
        float enemyGridX = x / TILE_SIZE;
        float enemyGridY = y / TILE_SIZE;
        float playerGridX = player.getX() / TILE_SIZE;
        float playerGridY = player.getY() / TILE_SIZE;

        // Check if the current path needs an update or if it's empty
        if (currentPath == null || currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            // Calculate a new path
            currentPath = pathfinder.findPath(enemyGridX, enemyGridY, playerGridX, playerGridY);
            pathIndex = 0; // Reset pathIndex to start from the beginning
        }
        followPath(delta);
    }

    /**
     * Marks the enemy as dead and starts playing the death animation.
     */

    public void die() {
        isDead = true;
        deathAnimationTime = 0; // Reset the animation timer
    }


    /**
     * Chooses a new direction for the enemy that is valid within the specified grid bounds and avoids collisions.
     *
     * @param maze The maze in which the enemy is moving.
     */
    private void chooseNewDirection(Maze maze) {
        Direction[] directions = Direction.values();
        Direction newDirection;
        boolean collision;

        do {
            newDirection = directions[random.nextInt(directions.length)];
            float projectedX = x, projectedY = y;
            float speed = TILE_SIZE;

            switch (newDirection) {
                case UP:
                    projectedY += speed;
                    break;
                case DOWN:
                    projectedY -= speed;
                    break;
                case LEFT:
                    projectedX -= speed;
                    break;
                case RIGHT:
                    projectedX += speed;
                    break;
            }

            collision = isCollisionWithWall(projectedX, projectedY, maze, currentDirection);

            // EnsureS that the new direction is not the same as the previous direction
            if (newDirection == currentDirection) {
                collision = true;
            }

        } while (collision);

        currentDirection = newDirection;
    }


    /**
     * Checks if the player character has entered a 3x3 grid area centered around the enemy.
     * The method calculates the bounds of this grid based on the enemy's current position
     * and checks if the player's bounding box overlaps with these bounds.
     *
     * @return true if the player character is within the grid, false otherwise.
     */
    private boolean playerEntersGrid() {
        // Define the range within which the player is considered to be in the enemy's range
        float detectionRadius = TILE_SIZE * 4; // For example, 4 tiles

        // Calculate the squared distance between the enemy and the player
        float distanceSquared = distanceSquared(x, y, player.getX(), player.getY());

        // Check if the distance is less than or equal to the detection radius squared
        return distanceSquared <= detectionRadius * detectionRadius;
    }

    /**
     * Handles the behavior when the enemy collides with a wall.
     * The enemy will choose a new direction to move in.
     */
    private void handleWallCollision() {
        if (currentState == EnemyState.PATROLLING) {
            // When patrolling, randomly choose a new direction
            Direction newDirection;
            do {
                newDirection = Direction.values()[random.nextInt(Direction.values().length)];
            } while (newDirection == currentDirection); // Ensure it's a different direction
            currentDirection = newDirection;
        } else if (currentState == EnemyState.CHASING) {
            // When chasing, prioritize the direction that gets closer to the player
            Direction playerDirectionX = (player.getX() > x) ? Direction.RIGHT : Direction.LEFT;
            Direction playerDirectionY = (player.getY() > y) ? Direction.UP : Direction.DOWN;

            // Randomly choose between X and Y directions with bias towards player's direction
            if (random.nextFloat() < 0.7f) {
                currentDirection = playerDirectionX;
            } else {
                currentDirection = playerDirectionY;
            }

            // Randomly choose between X and Y directions with bias away from player's direction
            if (random.nextFloat() < 0.7f) {
                currentDirection = playerDirectionX;
            } else {
                currentDirection = playerDirectionY;
            }
        }
    }


    /**
     * Updates the enemy's position and its bounding box.
     *
     * @param newX The new X-coordinate of the enemy.
     * @param newY The new Y-coordinate of the enemy.
     */
    public void setPosition(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        this.bounds.setPosition(newX, newY);
    }

    /**
     * Updates the state of the enemy.
     *
     * @param delta Time since last frame.
     */

    public void update(float delta) {

        stateTime += delta;

        // Check if the enemy is dead and play the death animation
        if (isDead) {
            deathAnimationTime += delta;
            if (deathAnimation.isAnimationFinished(deathAnimationTime)) {
                deathAnimationPlayed = true; // Mark the animation as completed

            }
            return;
        }
        // Check if the player has entered the enemy's grid
        if (playerEntersGrid() && currentState != EnemyState.CHASING) {
            currentState = EnemyState.CHASING;
            currentPath = null;
            pathIndex = 0;
        } else if (!playerEntersGrid() && currentState != EnemyState.PATROLLING) {
            currentState = EnemyState.PATROLLING;
        }
        switch (currentState) {
            case PATROLLING:
                patrol(delta, maze);
                break;
            case CHASING:
                chase(delta);
                break;
        }
    }


    /**
     * Draws the enemy at its current position using the appropriate animation frame.
     *
     * @param batch The SpriteBatch used for drawing.
     */
    public void draw(SpriteBatch batch) {
        if (isDead) {
            if (!deathAnimationPlayed) {
                TextureRegion currentFrame = deathAnimation.getKeyFrame(deathAnimationTime, false);
                batch.draw(currentFrame, x, y, TILE_SIZE, TILE_SIZE);
            } else {
                return;
            }
        }
        TextureRegion currentFrame = animations[currentDirection.ordinal()].getKeyFrame(stateTime, true);
        batch.draw(currentFrame, x, y, TILE_SIZE, TILE_SIZE);


    }


    /**
     * Calculates the squared distance between two points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @return The squared distance between the two points.
     */
    private float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }
    /**
     * Follows the current path by moving towards the next node in the path.
     *
     * @param delta The time passed since the last frame.
     */
    private void followPath(float delta) {
        if (currentPath != null && pathIndex < currentPath.size()) {
            Node nextNode = currentPath.get(pathIndex);
            int targetX = nextNode.x * TILE_SIZE;
            int targetY = nextNode.y * TILE_SIZE;

            moveTowards(targetX, targetY, delta);
            if (reachedNode(targetX, targetY)) {
                pathIndex++;
            }
        }
    }


    /**
     * Determines if the enemy has reached a specified node.
     *
     * @param targetX The x-coordinate of the node.
     * @param targetY The y-coordinate of the node.
     * @return true if the enemy has reached the node, false otherwise.
     */
    private boolean reachedNode(float targetX, float targetY) {
        return distanceSquared(x, y, targetX, targetY) < REACHED_NODE_TOLERANCE;
    }

    /**
     * Moves the enemy towards a specified target position.
     *
     * @param targetX The x-coordinate of the target position.
     * @param targetY The y-coordinate of the target position.
     * @param delta   The time passed since the last frame.
     */
    private void moveTowards(float targetX, float targetY, float delta) {
        float diffX = targetX - x;
        float diffY = targetY - y;
        float magnitude = (float) Math.sqrt(diffX * diffX + diffY * diffY);

        if (magnitude > 0) {
            float moveX = speed * delta * (diffX / magnitude);
            float moveY = speed * delta * (diffY / magnitude);

            // Ensure the enemy does not overshoot the target
            if (Math.abs(moveX) > Math.abs(diffX)) moveX = diffX;
            if (Math.abs(moveY) > Math.abs(diffY)) moveY = diffY;

            x += moveX;
            y += moveY;

            setPosition(x, y);
            // Calculate the direction based on the sign of diffX and diffY
            if (Math.abs(diffX) > Math.abs(diffY)) {
                currentDirection = (diffX > 0) ? Direction.RIGHT : Direction.LEFT;
            } else {
                currentDirection = (diffY > 0) ? Direction.UP : Direction.DOWN;
            }
        }
    }

}