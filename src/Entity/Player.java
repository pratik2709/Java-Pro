package Entity;

import Audio.AudioPlayer;
import TileMap.TileMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class Player extends MapObject {

    //GLIDING
    private boolean gliding;

    //player stuff
    private int health;
    private int maxHealth;
    private int fire;
    private int maxFire;
    private boolean dead;
    private boolean flinching;
    private long flinchTimer;

    //fireball
    private boolean firing;
    private int fireCost;
    private int fireBallDamage;
    private ArrayList<FireBall> fireBalls;

    //scratch
    private boolean scratching;
    private int scratchRange;
    private int scratchDamage;


    //animtaions
    private ArrayList<BufferedImage[]> sprites;

    //animation actions
    private final int[] numFrames = {
            2, 8, 1, 2, 4, 2, 5
    };

    private static final int IDLE = 0;
    private static final int WALKING = 1;
    private static final int JUMPING = 2;
    private static final int FALLING = 3;
    private static final int GLIDING = 4;
    private static final int FIREBALL = 5;
    private static final int SCRATCHING = 6;

    private HashMap<String, AudioPlayer> sfx;

    public Player(TileMap tm) {
        super(tm);
        //read from spritesheet
        width = 30;
        height = 30;

        //real
        cwidth = 20;
        cheight = 20;
        moveSpeed = 0.3;
        maxSpeed = 1.6;
        stopSpeed = 0.4;
        fallSpeed = 0.15;
        maxFallSpeed = 4.0;
        jumpStart = -4.8;
        stopJumpSpeed = 0.3;

        facingRight = true;
        health = maxHealth = 5;
        fire = maxFire = 2500;
        fireCost = 200;
        fireBallDamage = 5;
        fireBalls = new ArrayList<FireBall>();


        scratchDamage = 8;
        scratchRange = 40;

        //load sprites
        try {
            BufferedImage spriteSheet = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Sprites/Player/playersprites.gif"
                    )
            );
            sprites = new ArrayList<BufferedImage[]>();
            for (int i = 0; i < 7; i++) {
                //create a new buffered image array for each loop iteration
                BufferedImage[] bi = new BufferedImage[numFrames[i]];
                for (int j = 0; j < numFrames[i]; j++) {
                    if (i != 6) {
                        bi[j] = spriteSheet.getSubimage(
                                j * width,
                                i * height,
                                width,
                                height
                        );
                    } else {
                        bi[j] = spriteSheet.getSubimage(
                                j * width * 2,
                                i * height,
                                width * 2,
                                height
                        );
                    }

                }
                sprites.add(bi);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        animation = new Animation();
        currentAction = IDLE;
        animation.setFrames(sprites.get(IDLE));
        animation.setDelay(400);

        sfx = new HashMap<String, AudioPlayer>();
        sfx.put("jump", new AudioPlayer("/SFX/jump.mp3"));
        sfx.put("scratch", new AudioPlayer("/SFX/scratch.mp3"));
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getFire() {
        return fire;
    }

    public int getMaxFire() {
        return maxFire;
    }

    public void setFiring() {
        firing = true;
    }

    public void setScratching() {
        scratching = true;
    }

    public void setGliding(boolean b) {
        gliding = b;
    }

    public void checkAttack(ArrayList<Enemy> enemies){
        //check various forms of attack on the enemy
        //loop through enemies
        for(int i = 0; i < enemies.size(); i++){
            Enemy e = enemies.get(i);

            if(scratching) {
                if (facingRight) {
                    if(e.getx() > x
                            && e.getx() < x + scratchRange
                            && e.gety() > y - height/2 &&
                            e.gety() < y + height/2){
                        e.hit(scratchDamage);
                    }
                }
                else{
                    if(e.getx() < x &&
                            e.getx() > x - scratchRange &&
                            e.gety() > y - height/2 &&
                            e.gety() < y + height/2){
                        e.hit(scratchDamage);
                    }
                }
            }
            //fireballs
            for(int j = 0; j < fireBalls.size(); j++){
                if(fireBalls.get(j).intersects(e)){
                    e.hit(fireBallDamage);
                    //set the hit flag
                    fireBalls.get(j).setHit();
                    break;
                }
            }

            //check enemy collision
            if(intersects(e)){
                hit(e.getDamage());
            }
        }


        //check for fireballs

    }

    public void hit(int damage) {
        if(flinching){
            return;
        }
        health -= damage;
        if(health < 0) health = 0;
        if(health == 0) dead = true;
        flinching = true;
        flinchTimer = System.nanoTime();
    }

    public void update() {
        //??
        //after setting the position of the sprite set the animations
        getNextPosition();
        checkTileMapCollision();
        setPosition(xtemp, ytemp);

        //check attack has stopped
        if(currentAction == SCRATCHING){
            if(animation.hasPlayedOnce()) scratching = false;
        }
        if(currentAction == FIREBALL){
            if(animation.hasPlayedOnce()) firing = false;
        }

        //fireball attack
        //consistently generated
        fire += 1;
        //cap the value
        if(fire > maxFire) fire = maxFire;
        if(firing && currentAction != FIREBALL){
            //?? -- will have 2500/200=12 fireballs in the array ??
            if(fire > fireCost){
                fire -= fireCost;
                FireBall fb = new FireBall(tileMap, facingRight);
                //same as the player location
                fb.setPosition(x, y);
                fireBalls.add(fb);
            }
        }

        //update fireballs until all fireballs are removed
        for(int i = 0; i < fireBalls.size(); i++){
            fireBalls.get(i).update();
            if(fireBalls.get(i).shouldRemove()){
                fireBalls.remove(i);
                //https://stackoverflow.com/questions/18688339/arraylist-remove-element-shifting-list-down
                i--;
            }
        }

        //check done flinching
        if(flinching){
            long elapsed = (System.nanoTime() - flinchTimer)/1000000;
            if(elapsed > 1000){
                flinching = false;
            }
        }

        //set animation
        if (scratching) {
            if (currentAction != SCRATCHING) {
                sfx.get("scratch").play();
                currentAction = SCRATCHING;
                animation.setFrames(sprites.get(SCRATCHING));
                //??
                animation.setDelay(50);
                //width of the sprite
                width = 60;
            }
        } else if (firing) {
            if (currentAction != FIREBALL) {
                currentAction = FIREBALL;
                animation.setFrames(sprites.get(FIREBALL));
                animation.setDelay(100);
                width = 30;
            }
        }
        //dy is direction ?
        //falling animations
        //dy up means going down
        else if (dy > 0) {
            if (gliding) {
                if (currentAction != GLIDING) {
                    currentAction = GLIDING;
                    animation.setFrames(sprites.get(GLIDING));
                    //??
                    animation.setDelay(100);
                    //width of the sprite
                    width = 30;
                }
            } else if (currentAction != FALLING) {
                currentAction = FALLING;
                animation.setFrames(sprites.get(FALLING));
                animation.setDelay(100);
                width = 30;
            }

        }
        //jumping
        //if -ve dy is added to y things would go up
        else if (dy < 0) {
            if (currentAction != JUMPING) {
                currentAction = JUMPING;
                animation.setFrames(sprites.get(JUMPING));
                //since only 1 sprite??
                animation.setDelay(-1);
                width = 30;
            }
        }
        else if (left || right) {
            if (currentAction != WALKING) {
                currentAction = WALKING;
                animation.setFrames(sprites.get(WALKING));
                animation.setDelay(40);
                width = 30;
            }
        } else {
            //Idle frames
            if (currentAction != IDLE) {
                currentAction = IDLE;
                animation.setFrames(sprites.get(IDLE));
                animation.setDelay(400);
                width = 30;
            }
        }
        //moves stuff forward
        animation.update();

        //dint understand at all***
        //set the direction
        //???
        //dont want player to move when he is attacking
        if (currentAction != SCRATCHING && currentAction != FIREBALL) {
            //what is right and left and where it is first set??
            if (right) facingRight = true;
            if (left) facingRight = false;
        }
    }

    public void draw(Graphics2D g) {
        //?? should be called in every map object why ?
        setMapPostion();

        //draw fireballs
        for(int i = 0; i < fireBalls.size(); i++){
            fireBalls.get(i).draw(g);
        }

        //draw player
        if (flinching) {
            long elapsed = (System.nanoTime() - flinchTimer) / 1000000;
            //why ??
            //gives an appearance of blinking every 100 miliseconds
            //why divide by 100?
            if (elapsed / 100 % 2 == 0) {
                return;
            }
        }
        super.draw(g);
    }

    private void getNextPosition() {
        //movement
        //dont go beyond maximum speed
        //dx is the delta distance ?? -- sounds reasonable
        if (left) {
            dx -= moveSpeed;
            if (dx < -maxSpeed) {
                dx = -maxSpeed;
            }
        } else if (right) {
            dx += moveSpeed;
            if (dx > maxSpeed) {
                dx = maxSpeed;
            }
        } else {
            //?? -- if not left or right
            // left and right above have some meaning?
            if (dx > 0) {
                dx -= stopSpeed;
                if (dx < 0) {
                    dx = 0;
                }
            } else if (dx < 0) {
                dx += stopSpeed;
                if (dx > 0) {
                    dx = 0;
                }
            }
        }

        //cannot move while attacking expect in air
        if ((currentAction == SCRATCHING || currentAction == FIREBALL) && !(jumping || falling)) {
            //cannot move
            dx = 0;
        }

        //jumping
        //why not falling is here??
        //modification
        if (jumping && !falling) {
            sfx.get("jump").play();
            dy = jumpStart;
            falling = true;
        }

        //??
        //falling -- not standing on solid ground
        if (falling) {
            if (dy > 0 && gliding) {
                //fall at 10% of fall speed
                dy += fallSpeed * 0.1;
            } else {
                //fall at regular speed
                dy += fallSpeed;
            }
            //cant jump while falling !
            if (dy > 0) jumping = false;
            //jump button is not pressed and we are going up... come down!
            //the longer the jump button the higer it goes
            if (dy < 0 && !jumping) dy += stopJumpSpeed;

            if (dy > maxFallSpeed) dy = maxFallSpeed;
        }
    }
}
