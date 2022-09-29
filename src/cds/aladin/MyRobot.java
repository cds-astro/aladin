// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

/*
 * Created on 2 févr. 2004
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import cds.tools.Util;


/**
 * <p>Title : MyRobot</p>
 * <p>Description : Main class of the "robot" facility</p>
 * This is the main class of the "robot" facility<br>
 * It aims at managing script commands, translate them into simple tasks, and execute these tasks<br>
 * eg : script command "contour 4" would be translated into simple commands as "click on contour button", select 4, press Get contour<br>
 * Those commands would then be executed
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class MyRobot {


    static Robot robot;
    static boolean robotCompatible = true;


    static ActionExecutor ae;

    Aladin a;

    private ScriptFactory scriptFact;

    /** Constructor for MyRobot class */
    public MyRobot(Aladin a) {
        createRobot();
        scriptFact = new ScriptFactory();
        this.a = a;
    }

    /** Crée l'object robot statique */
    static public void createRobot()
    {
        if( robot!=null ) return;
        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        catch(AWTException e) {robotCompatible=false;}
    }

    /** Donne le focus au component chargé d'écouter pour interrompre le robot */
    /*
    static void gainFocus() {
        Aladin.aladin.logo.requestFocus();
    }
    */

    /** Ask the robot to execute a command, given its name and its arguments
     * @param cmdName command name, as it appears in class Command
     * @param argsStr arguments string
     * @return true si on a trouvé un script correspondant
     */
    public boolean executeCommand(String cmdName, String argsStr) {
        TranslationScript script = scriptFact.getScript(cmdName, argsStr);

        if( script==null ) {
            Aladin.trace(3, "No robot task found for command "+cmdName+".\n"+
            		        cmdName+" will be executed as a normal script command.");
            return false;
        }

        ae = new ActionExecutor(script.actions, a);
        ae.execute();

        return true;
    }

    // cas special pour commande info
    public static void info(String text, final Aladin aladinInst) {
        JFrame robotInfo = aladinInst.command.robotInfo;
        JTextArea infoTxt = aladinInst.command.infoTxt;

        // initialisation de la frame si necessaire
        if( aladinInst.command.robotInfo==null ) {
            aladinInst.command.robotInfo = new JFrame() {
                public boolean handleEvent(Event e) {
                    // pour sortir du mode Robot
                    if( e.id==Event.KEY_PRESS && e.key==java.awt.event.KeyEvent.VK_ESCAPE && aladinInst.command.robotMode ) {
                        aladinInst.stopRobot(this);
                        return true;
                    }
                    if( (e.id== Event.WINDOW_DESTROY ) ) hide();
                    return super.handleEvent(e);
                }
            };
            Util.setCloseShortcut(aladinInst.command.robotInfo, false, aladinInst);
            Aladin.setIcon(aladinInst.command.robotInfo);

            robotInfo = aladinInst.command.robotInfo;
            robotInfo.setLayout(new BorderLayout(0,10));
//            robotInfo.setBackground(Aladin.BKGD);
            robotInfo.add(new JScrollPane(aladinInst.command.infoTxt = new JTextArea("",10,50)), BorderLayout.CENTER);
            aladinInst.command.infoTxt.setWrapStyleWord(true);
            aladinInst.command.infoTxt.setLineWrap(true);
            infoTxt = aladinInst.command.infoTxt;
            JLabel l = new JLabel("Press [ESC] to stop the demonstration",JLabel.CENTER);
            l.setFont(Aladin.BOLD);
            l.setBackground(Aladin.BLUE);
            robotInfo.add(l, BorderLayout.SOUTH);
            robotInfo.pack();
//            robotInfo.setLocation(new ComponentLocator().getLocation(Aladin.aladin.view));
            robotInfo.setLocation(0,100);

            infoTxt.setFont(Aladin.BOLD);
            infoTxt.setForeground(Color.blue);
        }
        String title =  aladinInst.command.curTuto != null ?
                        aladinInst.command.curTuto : "Info";
        if( !robotInfo.getTitle().equals(title) ) robotInfo.setTitle(title);

        robotInfo.setVisible(true);
        robotInfo.toFront();

        FilterProperties.insertInTA(infoTxt, "\n\n", infoTxt.getText().length());
        type(text, infoTxt);

//        MyRobot.pause(4000);   // Commenté PF 7/7/06 ... en fait c'est déjà très lent
        // necessite de refaire passer Aladin.f en avant plan
        // (si la frame info est devant le bouton Load par exemple)
        //Aladin.f.toFront();

    }



    static Point lastLoc;
    static final int NBSTEPS100 = 10; // nb de pas pour une distance de 100 pixels


    private static double dist(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x-p2.x,2)+Math.pow(p1.y-p2.y,2));
    }

    protected void reset() {
    	lastLoc = null;
    }

    /** Mouve mouse cursor to point (px,py)
     *
     * @param loc
     */
    public static void moveTo(int px, int py, Aladin aladinInst) {
        if( !robotCompatible ) {
            System.out.println("Can't create java.awt.Robot object");
            return;
        }

        Point loc = new Point(px, py);

        if( lastLoc==null ) {
        	// start position : center of Aladin frame
        	ComponentLocator cl = new ComponentLocator();
        	Point p = cl.getLocation(aladinInst);
        	Dimension d = aladinInst.f.getSize();
        	lastLoc = new Point(p.x+d.width/2, p.y+d.height/2);
        }

        int x,y;
        int nbSteps = (int) dist(loc, lastLoc)*NBSTEPS100/100;
        for( int i=0; i<nbSteps ; i++ ) {
            x = lastLoc.x+(i*(loc.x-lastLoc.x))/nbSteps;
            y = lastLoc.y+(i*(loc.y-lastLoc.y))/nbSteps;
            robot.mouseMove(x, y);
            // on ralentit sur la fin
            if( i>=0.9*nbSteps || i<=0.1*nbSteps) robot.delay(80);
            else robot.delay(30);
        }

        // pour etre sur !!
        robot.mouseMove(loc.x, loc.y);
        lastLoc = loc;
    }

    /** Press the mouse button at point (x,y)
     *
     * @param x
     * @param y
     * @param comp
     */
    public static void press(int x, int y, Component comp) {
        press(new Point(x,y), comp);
    }

    /** Press the mouse button at point loc
     *
     * @param loc
     * @param delay
     * @param comp
     */
    private static void press(Point loc, int delay, Component comp) {
        if( !robotCompatible ) {
            System.out.println("Can't create java.awt.Robot object");
            return;
        }

        // le deplacement se fait en 2 temps pour laisser le temps au toFront d'etre effectif
        robot.mouseMove(loc.x/2, loc.y/2);
        if( comp!=null ) {
            Component parent = getRootParent(comp);
            if( parent instanceof JFrame ) {
                JFrame f = (JFrame)parent;
                f.toFront();
            }
        }
        robot.mouseMove(loc.x, loc.y);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    public static void press(Point loc, Component comp) {
        press(loc, 1500, comp);
    }

    /** Select a given item in a Choice widget
     *
     * @param item item to select
     * @param comp Choice component
     */
    public static void select(String item, Component comp, Point loc) {
        if( ! (comp instanceof JComboBox) ) return;

        press(loc, 0, comp);
        robot.delay(300);

        Integer myIntItem = null;
        boolean isInteger = true;
        try {
        	myIntItem = new Integer(item);
        }
        catch(NumberFormatException nfe) {
        	isInteger = false;
        }

        if( isInteger ) ((JComboBox)comp).setSelectedItem(myIntItem);
        else ((JComboBox)comp).setSelectedItem(item);


        // quelques tours de passe-passe pour que ça fonctionne !

        // pour Linux
        comp.doLayout();

        // pour Windows
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        robot.delay(300);
        comp.repaint();

        // abandonné, ne fonctionne pas bien sur tous les OS
        /*
        press(loc, 0, comp);
        ((Choice)comp).select(item);
        // necessaire pour que ça fonctionne sur toutes (?) les plateformes
        robot.keyRelease(KeyEvent.VK_ENTER);
        comp.repaint();
        */

    }

    /** Put a frame in the front
     *
     * @param comp Frame to put toFront
     */
    public static void toFront(Component comp) {
        if( ! (comp instanceof JFrame) ) return;

        ((JFrame)comp).toFront();
    }

    /** Type some text
     *
     * @param text text to type in
     */
    public static void type(String text, Component c) {
        if( !robotCompatible ) {
            System.out.println("Can't create java.awt.Robot object");
            return;
        }

        if( c!=null ) c.requestFocus();


        // just to trigger some potential events related to keyDown
        if( c instanceof JTextField || c instanceof JTextArea ) {
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
            robot.keyPress(KeyEvent.VK_BACK_SPACE);
            robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        }

        Random generator = new Random();
        for( int i=0; i<text.length(); i++ ) {
            if( c!=null ) {
                if( c instanceof JTextField ) {
                    ((JTextField)c).setText(text.substring(0,i));
                    ((JTextField)c).setCaretPosition(i);
                }
                else if( c instanceof JTextArea ) {
                    ((JTextArea)c).setText(text.substring(0,i));
                    ((JTextArea)c).setCaretPosition(i);
                }
                int delay = generator.nextInt(140)+50;
                if( delay>160 ) delay+=50;
                robot.delay(delay);
            }
        }

        if( c!=null ) {
            if( c instanceof JTextField ) {
                ((JTextField)c).setText(text);
                ((JTextField)c).setCaretPosition(text.length());
            }
            else if( c instanceof JTextArea ) {
                ((JTextArea)c).setText(text);
                ((JTextArea)c).setCaretPosition(text.length());
            }
        }
    }

    /**
     * Ecrit text a la fin du texte deja present (conserve le texte original)
     * @param text texte a ecrire
     * @param ta objet TextArea dans lequel on ecrit
     */
    private static void type(String text, JTextArea ta) {
        if( !robotCompatible ) {
            System.out.println("Can't create java.awt.Robot object");
            return;
        }

        if( ta==null ) return;

        Random generator = new Random();
        for( int i=0; i<text.length(); i++ ) {
            ta.append(""+text.charAt(i));
            ta.setCaretPosition(ta.getDocument().getLength() );
            int delay = generator.nextInt(140)+50;
            if( delay>160 ) delay+=50;
            robot.delay(delay);
        }
        ta.setCaretPosition(ta.getDocument().getLength() );
    }

    private static Dimension screenSize;
    /** Adjusts the position of a frame, so that it is fully visible on the screen
     *
     * @param comp frame
     */
    public static void adjustPos(Component comp) {
        if( screenSize==null ) screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if( ! (comp instanceof JFrame) ) return;
        JFrame f = (JFrame)comp;
        Point pos = f.getLocation();
        Dimension size = f.getSize();
        if( pos.x<0 || pos.y<0 || pos.x+size.width>screenSize.width || pos.y+size.height>screenSize.height ) {
            f.setLocation(screenSize.width/2-size.width/2, screenSize.height/2-size.height/2);
        }
        f.toFront();
    }

    /** Pause during a given time
     *
     * @param nbMs number of ms to pause
     */
    public static void pause(int nbMs) {
        if( !robotCompatible ) {
            System.out.println("Can't create java.awt.Robot object");
            return;
        }

        robot.delay(nbMs);
    }

    /** returns the root parent (should be a frame) of a component
     *
     * @param comp
     * @return Component
     */
    private static Component getRootParent(Component comp) {
        Component curComp = comp;
        while( curComp.getParent()!=null )
            curComp = curComp.getParent();
        return curComp;
    }

}
