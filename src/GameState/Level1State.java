package GameState;

import Entity.*;
import Main.GamePanel;
import TileMap.*;

import java.awt.*;

public class Level1State extends GameState {

    private TileMap tileMap;
    private Background bg;

    private Player player;

    public Level1State(GameStateManager gsm) {
        this.gsm = gsm;
        init();
    }

    @Override
    public void init() {
        tileMap = new TileMap(30);
        tileMap.loadTiles("/Tilesets/grasstileset.gif");
        tileMap.loadMap("/Maps/level1-1.map");
        tileMap.setPosition(0,0);

        bg = new Background("/Backgrounds/grassbg1.gif", (int) 0.1);

        player = new Player(tileMap);
    }

    @Override
    public void update() {
        player.update();

    }

    @Override
    public void draw(Graphics2D g) {
        bg.draw(g);
        tileMap.draw(g);
        //draw player
        player.draw(g);
    }

    @Override
    public void keyReleased(int k) {

    }

    @Override
    public void keyPressed(int k) {

    }
}
