import acm.graphics.*;
import acm.program.*;
import acm.util.RandomGenerator;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import java.lang.Math;

public class FinalProject extends GraphicsProgram {
	
	public static final int APPLICATION_WIDTH = 1000;
	public static final int APPLICATION_HEIGHT = 700;
	public static final int DELAY = 20;
	public static final double GRAVITATION = 0.5;
	public static final int GROUND_HEIGHT = 30;
	public static final int MONSTER_SIZE = 50;
	public static final int BON_SIZE = 40;
	
	RandomGenerator rgen = new RandomGenerator();
	
	GImage hero = null;
	GImage sky = null;
	GImage ground = null;
	int bulletSpeed = -5;
	
	//database of monsters, their speeds and sizes
	ArrayList<GImage> monsters = new ArrayList<GImage>();
	ArrayList<Double> monX = new ArrayList<Double>();
	ArrayList<Double> monY = new ArrayList<Double>();
	ArrayList<Integer> monSize = new ArrayList<Integer>();
	ArrayList<GOval> bonus = new ArrayList<GOval>();
	
	//database of bullets
	ArrayList<GImage> bullets = new ArrayList<GImage>();

	public void run() {
		//introduction
		introduction();
		
		//create environment
		background();
		addHero();
		//start to listen to the mouse
		addMouseListeners();
		//create and add monsters + setting variables for their movement
		level1();
		//3 lives
		int hp = 3;
		int level = 1;
		
		while(true) {
			//monsters's movement
			if(monstersMove()) {
				hp--;
				if(hp==0)
					break;
				startAgain(hp, level);
				continue;
			}
			
			//bullets's movement
			bulletsMove();
			
			//move with bonuses, if there are some
			if(bonus.size() > 0) {
				bonusMove();
			}
			
			//level completed? level up
			if(monsters.size() == 0) {
				levelComplete(level);
				level++;
				switch (level) {
					case 2:	level2();
							break;
					case 3:	level3();
							break;
					case 4:	level4();
							break;
					case 5:	level5();
							break;
					default: break;
				}
				if(level>5)
					break;
			}
			pause(DELAY);
		}
		if(hp == 0)
			gameOver();
		else
			winner();
	}
	
	/** MOVES WITH EVERYTHING AND CONTROLS TOUCHES **/
	
	//moves with all monsters, controls if the monster has touched the hero, if it has, returns true
	private boolean monstersMove() {
		GObject hit1 = null;
		for(int i=0; i<monsters.size(); i++) {
			//bouncing from the floor
			if(monsters.get(i).getY() >= APPLICATION_HEIGHT-monsters.get(i).getHeight()-GROUND_HEIGHT-10)
				monY.set(i, -monY.get(i));
			//bouncing from the walls
			if(monsters.get(i).getX() > APPLICATION_WIDTH-monsters.get(i).getWidth()  ||  monsters.get(i).getX() < 0)
				monX.set(i, -monX.get(i));
			//gravitation strikes
			monY.set(i, monY.get(i)+GRAVITATION);
			//movement
			monsters.get(i).move(monX.get(i), monY.get(i));
			
			//touched the hero?
			//point on the bottom in the middle of the monster
			if((hit1 = getElementAt(monsters.get(i).getX()+monsters.get(i).getWidth()/2, monsters.get(i).getY()+monsters.get(i).getHeight())) != null
					&& hit1 == hero) {
				return true;
			}
		}
		
		return false;
	}
	
	//moves with all bullets and controls if monster was hit
	private void bulletsMove() {
		GObject hit = null;
		
		//go through the list of bullets and move with them
		if(bullets.size() == 0)
			return;
		for(int i=(bullets.size()-1); i>=0; i--) {
			bullets.get(i).move(0, bulletSpeed);
			//bullet isn't on the screen - remove it
			if(bullets.get(i).getY() < -bullets.get(i).getHeight()) {
				bullets.remove(i);
				continue;
			}
			//bullet hits something
			if((hit = getElementAt(bullets.get(i).getX()+bullets.get(i).getWidth()/2, bullets.get(i).getY()-1)) != null && hit != sky) {
				monsterHit(hit, i);
			}else if((hit = getElementAt(bullets.get(i).getX()+bullets.get(i).getWidth()/2, bullets.get(i).getY()+bullets.get(i).getHeight())) != null && hit != sky && hit != hero) {
				monsterHit(hit, i);
			}
		}
	}
	
	//bullet hits monster
	private void monsterHit(GObject hit, int i) {
		for(int j=0; j<monsters.size(); j++) {
			if(monsters.get(j) == hit) {
				//drop bonus with a 30% probability
				if(rgen.nextBoolean(0.3))
					bonus(monsters.get(j).getX(), monsters.get(j).getY());
				
				//delete monster and the bullet from the screen
				remove(monsters.get(j));
				remove(bullets.get(i));
				
				//if monster size 1 was destroyed, just delete it, if bigger create two smaller ones
				if(monSize.get(j) == 1){
					monsters.remove(j);
					monX.remove(j);
					monY.remove(j);
					monSize.remove(j);
					bullets.remove(i);
					return;
				}else {
					//create two smaller monsters
							
					//first
					monsters.add(createMonster(monSize.get(j)-1));
					//to where it will be moving 
					if(monX.get(j) > 0) {
						monX.add(-monX.get(j));
					} else {
						monX.add(monX.get(j));
					}
					monY.add(monY.get(j));
					add(monsters.get(monsters.size()-1), monsters.get(j).getX(), 
							monsters.get(j).getY()+monsters.get(j).getHeight()/2-monsters.get(monsters.size()-1).getHeight()/2);
							
					//second
					monsters.add(createMonster(monSize.get(j)-1));
					monX.add(-monX.get(monX.size()-1));
					monY.add(monY.get(j));
					add(monsters.get(monsters.size()-1), monsters.get(j).getX()+monsters.get(j).getWidth()/2, 
							monsters.get(j).getY()+monsters.get(j).getHeight()/2-monsters.get(monsters.size()-1).getHeight()/2);
							
					//delete the bullet and the hit monster
					monsters.remove(j);
					monX.remove(j);
					monY.remove(j);
					monSize.remove(j);
					bullets.remove(i);
				}
			}
		}
	}
	
	private void bonusMove() {
		GObject hit = null;
		for(int i=bonus.size()-1; i>=0; i--) {
			bonus.get(i).move(0,  10);
			//control whether the bonus was grabbed (left, right and bottom of the ball)
			if((hit = getElementAt(bonus.get(i).getX()+BON_SIZE/2, bonus.get(i).getY()+BON_SIZE)) == hero 
					|| (hit = getElementAt(bonus.get(i).getX(), bonus.get(i).getY()+BON_SIZE/2)) == hero
					|| (hit = getElementAt(bonus.get(i).getX()+BON_SIZE, bonus.get(i).getY()+BON_SIZE/2)) == hero) {
				bulletSpeed -= 3;
				remove(bonus.get(i));
				bonus.remove(i);
				return;
			}
			//bonus will disappear when it touches the ground
			if(bonus.get(i).getY() > APPLICATION_HEIGHT-GROUND_HEIGHT-bonus.get(i).getHeight()) {
				remove(bonus.get(i));
				bonus.remove(i);
			}
		}
	}
	
	private void bonus(double x, double y) {
		GOval bon = new GOval(BON_SIZE, BON_SIZE);
		bon.setFilled(true);
		bon.setColor(Color.RED);
		add(bon, x, y);
		
		bonus.add(bon);
	}
	
	
	/** MESSAGES TO PLAYER AND PHASES OF THE GAME **/
	
	//introduction to the story and how to control the game
	private void introduction() {
		GImage intro = new GImage("res\\intro.png");
		add(intro, 0, 0);
		
		GLabel t1 = new GLabel("Your village was attacted by dreadful monsters!");
		t1.setFont("BookmanOldStyle-30");
		t1.setColor(Color.WHITE);
		add(t1, APPLICATION_WIDTH/2-t1.getWidth()/2, 50);
		pause(3000);
		remove(t1);
		
		GLabel t2 = new GLabel("You are the only one who is able to save it!");
		t2.setFont("BookmanOldStyle-30");
		t2.setColor(Color.WHITE);
		add(t2, APPLICATION_WIDTH/2-t2.getWidth()/2, 50);
		pause(3000);
		remove(t2);
		
		GLabel control = new GLabel("movement of the mouse = movement of the hero (you)");
		control.setFont("BookmanOldStyle-30");
		control.setColor(Color.WHITE);
		add(control, APPLICATION_WIDTH/2-control.getWidth()/2, 50);
		
		//right edge of all text
		double x = (APPLICATION_WIDTH/2+control.getWidth()/2);
		
		GLabel control2 = new GLabel("click = shooting");
		control2.setFont("BookmanOldStyle-30");
		control2.setColor(Color.WHITE);
		add(control2, x-control2.getWidth(), 100);
		
		GOval showB = new GOval(BON_SIZE, BON_SIZE);
		showB.setFilled(true);
		showB.setColor(Color.RED);
		add(showB, x-50, 500);
		
		GLabel aboutB = new GLabel("Catch me, I'm a bonus");
		aboutB.setFont("BookmanOldStyle-30");
		aboutB.setColor(Color.WHITE);
		add(aboutB, x-aboutB.getWidth(), 600);
		
		GLabel aboutB2 = new GLabel("and I will speed up your bullets!");
		aboutB2.setFont("BookmanOldStyle-30");
		aboutB2.setColor(Color.WHITE);
		add(aboutB2, x-aboutB2.getWidth(), 650);
		
		pause(5000);
		remove(control2);
		remove(control);
		remove(intro);
	}
	
	private void levelComplete(int level) {
		//deletes all remaining bonuses
		for(int i=bonus.size()-1; i>=0; i--) {
			remove(bonus.get(i));
			bonus.remove(i);
		}
		//deletes all remaining bullets
		for(int i=bullets.size()-1; i>=0; i--) {
			remove(bullets.get(i));
			bullets.remove(i);
		}

		Color clr = new Color(255, 110, 74, 150);
		
		GRect screen = new GRect(APPLICATION_WIDTH, APPLICATION_HEIGHT);
		screen.setFilled(true);
		screen.setColor(clr);
		add(screen, 0, 0);
		
		GLabel text = new GLabel("Level " + level + " Complete!");
		text.setFont("BookmanOldStyle-40");
		add(text, APPLICATION_WIDTH/2-text.getWidth()/2, APPLICATION_HEIGHT/2-text.getHeight()/2);
		
		pause(3000);
		remove(text);
		
		if(level == 4) {
			finalBoss();
		}
		remove(screen);
	}
	
	//final boss message
	private void finalBoss() {
		GLabel text = new GLabel("Final boss!");
		text.setFont("BookmanOldStyle-40");
		add(text, APPLICATION_WIDTH/2-text.getWidth()/2, APPLICATION_HEIGHT/2-text.getHeight()/2);
		
		pause(3000);
		
		remove(text);
	}
	
	//if the hero was hit and player still has lives, the level starts again
	private void startAgain(int lives, int level) {
		//deletes all remaining monsters
		for(int i=monsters.size()-1; i>=0; i--) {
			remove(monsters.get(i));
			monsters.remove(i);
			monX.remove(i);
			monY.remove(i);
			monSize.remove(i);
		}
		//deletes all remaining bullets
		for(int i=bullets.size()-1; i>=0; i--) {
			remove(bullets.get(i));
			bullets.remove(i);
		}
		//deletes all remaining bonuses
		for(int i=bonus.size()-1; i>=0; i--) {
			remove(bonus.get(i));
			bonus.remove(i);
		}
		
		//message for the player
		Color transparent = new Color(0, 0, 0, 150);
		GRect screen = new GRect(APPLICATION_WIDTH, APPLICATION_HEIGHT);
		screen.setFilled(true);
		screen.setColor(transparent);
		add(screen, 0, 0);
		
		GLabel minusLife = new GLabel("You've got hit! Remaining lives: " + lives);
		minusLife.setFont("BookmanOldStyle-40");
		minusLife.setColor(Color.RED);
		add(minusLife, APPLICATION_WIDTH/2-minusLife.getWidth()/2, APPLICATION_HEIGHT/2-minusLife.getHeight()/2);
		
		pause(3000);
		
		remove(screen);
		remove(minusLife);
		
		switch (level) {
			case 2:	level2();
					break;
			case 3:	level3();
					break;
			case 4:	level4();
					break;
			case 5:	level5();
					break;
			default: break;
		}
	}
	
	//game is completed
	private void winner() {
		//deletes all remaining bonuses
		for(int i=bonus.size()-1; i>=0; i--) {
			remove(bonus.get(i));
			bonus.remove(i);
		}
		//deletes all remaining bullets
		for(int i=bullets.size()-1; i>=0; i--) {
			remove(bullets.get(i));
			bullets.remove(i);
		}
		
		GImage winner = new GImage("res\\winner.jpg");
		add(winner, 0, 0);
		
		GLabel text = new GLabel("CONGRATULATIONS!");
		text.setFont("BookmanOldStyle-40");
		add(text, 400, 120);
		
		GLabel story1 = new GLabel("You've destroyed all monsters.");
		story1.setFont("BookmanOldStyle-30");
		add(story1, text.getX()+text.getWidth()/2-story1.getWidth()/2, 200);
		
		GLabel story2 = new GLabel("Your village is safe now.");
		story2.setFont("BookmanOldStyle-30");
		add(story2, text.getX()+text.getWidth()/2-story2.getWidth()/2, 250);
		
		GLabel story3 = new GLabel("Be proud of yourself!");
		story3.setFont("BookmanOldStyle-30");
		add(story3, text.getX()+text.getWidth()/2-story3.getWidth()/2, 300);
	}
	
	//shows a message about ending of the game
	private void gameOver() {
		//deletes all remaining bonuses
		if(bonus.size()>0) {
			for(int i=bonus.size()-1; i>=0; i--) {
				remove(bonus.get(i));
				bonus.remove(i);
			}
		}
		//deletes all remaining bullets
		for(int i=bullets.size()-1; i>=0; i--) {
			remove(bullets.get(i));
			bullets.remove(i);
		}
		
		GRect end = new GRect(APPLICATION_WIDTH, APPLICATION_HEIGHT);
		end.setFilled(true);
		for(int i=0; i<=255; i+=5) {
			Color endClr = new Color(0, 0, 0, i);
			end.setColor(endClr);
			add(end, 0, 0);
			
			pause(50);
		}
		
		GLabel endMessage = new GLabel("GAME OVER");
		endMessage.setFont("BookmanOldStyle-40");
		endMessage.setColor(Color.WHITE);
		add(endMessage, APPLICATION_WIDTH/2-endMessage.getWidth()/2, 300);
		
		GLabel shame = new GLabel("Our village was destroyed and our people enslaved...");
		shame.setFont("BookmanOldStyle-30");
		shame.setColor(Color.white);
		add(shame, APPLICATION_WIDTH/2-shame.getWidth()/2, 350);
		
		GLabel shame2 = new GLabel("(shame on you)");
		shame2.setFont("BookmanOldStyle-30");
		shame2.setColor(Color.white);
		add(shame2, APPLICATION_WIDTH/2-shame2.getWidth()/2, 400);
	}
	
	
	/** LEVELS **/
	
	//configuration for level 1
	private void level1() {
		monsters.add(createMonster(1));
		monX.add(5.0);
		monY.add(0.0);
		add(monsters.get(0), 0, 50);
	}
	
	//configuration for level 2
	private void level2() {
		monsters.add(createMonster(2));
		monX.add(7.0);
		monY.add(0.0);
		add(monsters.get(0), 0, 50);
			
		monsters.add(createMonster(1));
		monX.add(-5.0);
		monY.add(0.0);
		add(monsters.get(1), 200, 100);
	}
	
	//configuration for level 3
	private void level3() {
		monsters.add(createMonster(3));
		monX.add(3.0);
		monY.add(0.0);
		add(monsters.get(0), 20, 0);
		
		monsters.add(createMonster(2));
		monX.add(-5.0);
		monY.add(0.0);
		add(monsters.get(1), 700, 50);
		
		monsters.add(createMonster(1));
		monX.add(7.0);
		monY.add(0.0);
		add(monsters.get(2), 500, 100);
	}
	
	//configuration for level 4
	private void level4() {
		monsters.add(createMonster(3));
		monX.add(8.0);
		monY.add(0.0);
		add(monsters.get(0), 510, 60);
		
		monsters.add(createMonster(3));
		monX.add(-6.0);
		monY.add(0.0);
		add(monsters.get(1), 60, 100);
		
		monsters.add(createMonster(2));
		monX.add(-5.0);
		monY.add(0.0);
		add(monsters.get(2), 500, 0);
	}
	
	//configuration for level 5
	private void level5() {
		//final boss
		monsters.add(createFinalBoss(4));
		monX.add(8.0);
		monY.add(0.0);
		add(monsters.get(0), 0, 0);
		
		monsters.add(createMonster(3));
		monX.add(10.0);
		monY.add(0.0);
		add(monsters.get(1), 510, 60);
		
		monsters.add(createMonster(3));
		monX.add(-10.0);
		monY.add(0.0);
		add(monsters.get(2), 800, 60);
	}
	
	/** THEY DO SOMETHING WHEN PLAYER DO SOMETHING WITH THE MOUSE **/ 
	
	//hero moves according to mouse movement
	public void mouseMoved(MouseEvent e) {
		hero.setLocation(e.getX()-hero.getWidth()/2, APPLICATION_HEIGHT-GROUND_HEIGHT-hero.getHeight());
		if(hero.getX() < 0)
			hero.setLocation(0, APPLICATION_HEIGHT-GROUND_HEIGHT-hero.getHeight());
		if(hero.getX() > APPLICATION_WIDTH-hero.getWidth())
			hero.setLocation(APPLICATION_WIDTH-hero.getWidth(), APPLICATION_HEIGHT-GROUND_HEIGHT-hero.getHeight());
	}
	
	//when player clicks, bullet appears and starts moving
	public void mouseClicked(MouseEvent e) {
		GImage bullet = new GImage("res\\bullet.png");
		bullet.scale(0.05);
		add(bullet, hero.getX(), hero.getY());
		bullets.add(bullet);
	}
		
	
	/** CREATING THINGS **/
	
	//creates monsters according to given size and coordinates
	private GImage createMonster(int size) {
		monSize.add(size);
		size--;
		GImage monster = new GImage("res\\monster.png");
		monster.setSize(Math.pow(2,size)*MONSTER_SIZE, Math.pow(2, size)*MONSTER_SIZE);
		
		return monster;
	}
	
	//creates final boss
	private GImage createFinalBoss(int size) {
		monSize.add(size);
		size--;
		GImage monster = new GImage("res\\final_boss.png");
		monster.setSize(Math.pow(2,size)*MONSTER_SIZE, Math.pow(2, size)*MONSTER_SIZE);
		
		return monster;
	}

	//creates hero and gives it to the middle
	private void addHero() {
		hero = new GImage("res\\hero3.png");
		hero.scale(0.2);
		add(hero, APPLICATION_WIDTH/2-hero.getWidth()/2, APPLICATION_HEIGHT-GROUND_HEIGHT-hero.getHeight());
	}
	
	//creates sky and ground
	private void background() {
		//create sky
		sky = new GImage("res\\background.png");
		add(sky, 0, 0);
		
		//create ground
		ground = new GImage("res\\ground2.png");
		add(ground, 0, APPLICATION_HEIGHT-GROUND_HEIGHT-25);
	}
}
