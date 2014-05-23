package de.hhu.propra.team61.Objects;

import com.sun.javafx.geom.Shape;
import de.hhu.propra.team61.IO.TerrainManager;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Polygon;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * A GridPane representing a terrain.
 * The class has methods for getting spawn points, walkability checks and destroying terrain (TODO actually not)
 * Created by markus on 17.05.14.
 */
public class Terrain extends GridPane {
    private static String imgPath = "file:resources/";

    private ArrayList<ArrayList<Character>> terrain;
    private ArrayList<Point2D> spawnPoints;

    /**
     * @param terrain 2-D-ArrayList containing the terrain to be displayed
     */
    public Terrain(ArrayList<ArrayList<Character>> terrain) {
        load(terrain);
    }

    public void load(ArrayList<ArrayList<Character>> terrain) {
        getChildren().clear();

        this.terrain = terrain;

        spawnPoints = new ArrayList<Point2D>();

        //Draw Terrain
        setGridLinesVisible(true); //Gridlines on/off
        setAlignment(Pos.TOP_LEFT);

        String img;

        for(int i=0; i < terrain.size(); i++){
            for(int j=0; j < terrain.get(i).size(); j++){
                char terraintype = terrain.get(i).get(j);
                switch(terraintype) {
                    case '_': img = "plain_ground.png";
                        break;
                    case '/': img = "slant_ground_ri.png";
                        break;
                    case '\\': img = "slant_ground_le.png";
                        break;
                    case '|': img = "wall_le.png";
                        break;
                    case 'S': img = "stones.png";
                        break;
                    case 'E': img = "earth.png";
                        break;
                    case 'W': img = "water.png";
                        break;
                    case 'I': img = "ice.png";
                        break;
                    case 'L': img = "lava.png";
                        break;
                    case 'P': // special case: spawn point, add to list and draw sky
                        spawnPoints.add(new Point2D(j, i));
                        terrain.get(i).set(j, ' ');
                    default : img = "sky.png";
                }
                Image image = new Image(imgPath + img);
                ImageView content = new ImageView();
                content.setImage(image);

                add(content, j, i);
                //terrainGrid.setConstraints(content,j,i);
            }
        }
    }

    /**
     * @return the 2-D-ArrayList representing the loaded terrain
     */
    public ArrayList<ArrayList<Character>> toArrayList() {
        return terrain;
    }

    /**
     * get a spawn point and remove it from the list of available spawn points
     * @return a random spawn point, or null if there are no more spawn points
     */
    public Point2D getRandomSpawnPoint() {
        if(spawnPoints.isEmpty()) {
            return null;
        }
        int index = (int) (Math.random()*spawnPoints.size());
        Point2D spawnPoint = spawnPoints.get(index);
        spawnPoints.remove(index);
        System.out.println("TERRAIN: returning spawn point #" + index + " " + spawnPoint + " (" + spawnPoints.size() + " left)");
        return spawnPoint;
    }

    /**
     * @param n
     * @return n random spawn points
     * @see #getRandomSpawnPoint()
     */
    public ArrayList<Point2D> getRandomSpawnPoints(int n) {
        ArrayList<Point2D> spawnPoints = new ArrayList<>();
        for(int i=0; i<n; i++) {
            spawnPoints.add(getRandomSpawnPoint());
        }
        return spawnPoints;
    }

    /**
     * @param hitRegion of a figure or whatever
     * @param c a terrain field code
     * @param x column of the field
     * @param y row of the field
     * @return true, when hitRegion intersects with the field
     */
    private boolean intersects(Rectangle2D hitRegion, char c, int x, int y) {
        switch(c) {
            case ' ':
                return false;
            case '/':
                Point2D p;
                for(int i=0; i<8; i++) {
                    int px = x*8+i;
                    int py = y*8+8-i;
                    p = new Point2D(px, py);
                    if(hitRegion.contains(p)) {
                        System.out.println("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            case '\\':
                for(int i=0; i<8; i++) {
                    int px = x*8+i;
                    int py = y*8+i;
                    p = new Point2D(px, py);
                    if(hitRegion.contains(p)) {
                        System.out.println("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            default:
                Rectangle2D rec = new Rectangle2D(x * 8, y * 8, 8, 8);
                return hitRegion.intersects(rec);
        }
    }

    /**
     * adds direction to oldPosition, but assures that we do not walk/fly through terrain or other figures
     * When stopOnEveryCollision is true, the movement is always stopped when hitting a object (used for munition); TODO update, parameters changed
     * otherwise, the movement may also continue in diagonal direction (used for figures)
     * @param oldPosition old position of the object
     * @param direction direction vector of the object
     * @param hitRegion
     * @param stopOnEveryCollision when true, also stop on diagonal walls
     * @return new position
     * @throws CollisionWithFigureException TODO
     * @throws CollisionWithTerrainException TODO
     * TODO dummy implementation, randomly throws exceptions
     */
    public Point2D getPositionForDirection(Point2D oldPosition, Point2D direction, Rectangle2D hitRegion, boolean canWalkAlongDiagonals) throws CollisionWithTerrainException {
        Point2D newPosition = new Point2D(oldPosition.getX(), oldPosition.getY());
        Point2D preferredFinalPosition = oldPosition.add(direction);
        Point2D normalizedDirection = direction.normalize();

        System.out.println("start position: " + oldPosition);
        System.out.println("normalized velocity: " + normalizedDirection);

        final int runs = (int)direction.magnitude();

        for(int i=0; i<runs; i++) {
            // move position by 1px
            newPosition = newPosition.add(normalizedDirection);

            // calculate moved hitRegion
            hitRegion = new Rectangle2D(hitRegion.getMinX()+normalizedDirection.getX(), hitRegion.getMinY()+normalizedDirection.getY(), hitRegion.getWidth(), hitRegion.getHeight());

            System.out.println("checking new position for collision: " + newPosition + " (" + (i+1) + "/" + runs +")" + " " + hitRegion);

            // check if hitRegion intersects with non-walkable terrain
            boolean triedDiagonal = false;
            int tries = 0;
            Point2D diagonalDirection = new Point2D(0, 0);
            do {
                triedDiagonal = false;
                System.out.println("try " + tries + " S");

                // calculate indices of fields which are touched by hitRegion
                int minY = (int) Math.floor(hitRegion.getMinY() / 8);
                int maxY = (int) Math.ceil(hitRegion.getMaxY() / 8);
                int minX = (int) Math.floor(hitRegion.getMinX() / 8);
                int maxX = (int) Math.ceil(hitRegion.getMaxX() / 8);

                // make sure that we are not out of bounds
                if (minY < 0) minY = 0;
                else if (maxY >= terrain.size()) maxY = terrain.size()-1;
                if (minX < 0) minX = 0;
                else if (maxX >= terrain.get(0).size()) maxX = terrain.get(0).size()-1;

                for (int y = minY; y <= maxY && !triedDiagonal; y++) { // TODO recheck necessity of <=
                    for (int x = minX; x <= maxX && !triedDiagonal; x++) {
                        //System.out.println(hitRegion + " " + terrain.get(y).get(x) + " field: " + rec);
                        if (intersects(hitRegion, terrain.get(y).get(x), x, y)) {
                            System.out.println("intersection at " + x + " " + y + " with " + terrain.get(y).get(x));
                            if (canWalkAlongDiagonals && tries == 0) {
                                diagonalDirection = new Point2D(Math.signum(normalizedDirection.getX()), -2);
                                Point2D positionOnSlope = newPosition.subtract(normalizedDirection).add(diagonalDirection);
                                hitRegion = new Rectangle2D(hitRegion.getMinX()-normalizedDirection.getX()+diagonalDirection.getX(), hitRegion.getMinY()-normalizedDirection.getY()+diagonalDirection.getY(), hitRegion.getWidth(), hitRegion.getHeight());
                                newPosition = positionOnSlope;
                                triedDiagonal = true;
                                System.out.println("trying to walk diagonal along " + diagonalDirection + " to " + newPosition + " " + hitRegion);
                            } else {
                                newPosition = newPosition.subtract(normalizedDirection).subtract(diagonalDirection);
                                newPosition = new Point2D(Math.floor(newPosition.getX()), Math.ceil(newPosition.getY())); // TODO code duplication
                                throw new CollisionWithTerrainException(newPosition);
                            }
                        }
                    }
                }
                System.out.println("try " + tries + " E");
            } while(triedDiagonal && ++tries<2);
            
            System.out.println("beforehitground: "+newPosition + " " + hitRegion);
            boolean hitGround = false;
            tries = 0;
            final double MOVING_GROUND_DELTA_Y = .1;
            do { // while(!hitGround)
                System.out.println("moving to ground ... " + (++tries));
                newPosition = newPosition.add(0, MOVING_GROUND_DELTA_Y);
                // calculate moved hitRegion
                hitRegion = new Rectangle2D(hitRegion.getMinX(), hitRegion.getMinY()+MOVING_GROUND_DELTA_Y, hitRegion.getWidth(), hitRegion.getHeight());
                System.out.println(newPosition + " " + hitRegion);

                // calculate indices of fields which are touched by hitRegion // TODO code duplication
                int minY = (int) Math.floor(hitRegion.getMinY() / 8);
                int maxY = (int) Math.ceil(hitRegion.getMaxY() / 8);
                int minX = (int) Math.floor(hitRegion.getMinX() / 8);
                int maxX = (int) Math.ceil(hitRegion.getMaxX() / 8);

                // make sure that we are not out of bounds
                if (minY < 0) minY = 0;
                else if (maxY >= terrain.size()) maxY = terrain.size()-1;
                if (minX < 0) minX = 0;
                else if (maxX >= terrain.get(0).size()) maxX = terrain.get(0).size()-1;

                for (int y = minY; y <= maxY && !hitGround; y++) { // TODO recheck necessity of <=
                    for (int x = minX; x <= maxX && !hitGround; x++) {
                        //System.out.println(hitRegion + " " + terrain.get(y).get(x) + " " + x + " " + y);
                        if (intersects(hitRegion, terrain.get(y).get(x), x, y)) {
                            newPosition = newPosition.subtract(0, MOVING_GROUND_DELTA_Y);
                            // calculate moved hitRegion
                            hitRegion = new Rectangle2D(hitRegion.getMinX(), hitRegion.getMinY()-MOVING_GROUND_DELTA_Y, hitRegion.getWidth(), hitRegion.getHeight());
                            hitGround = true;
                        }
                    }
                }
                if(tries > 200) {
                    System.out.println("WARNING stopped movement, probably an error");
                    break;
                }
            } while(!hitGround);

        } // for i<runs

        newPosition = new Point2D(Math.floor(newPosition.getX()), Math.ceil(newPosition.getY())); // TODO code duplication
        return newPosition;
    }

}