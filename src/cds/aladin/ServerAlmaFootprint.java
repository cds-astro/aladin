package cds.aladin;

import java.awt.Dimension;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ServerAlmaFootprint extends Server {

    protected ServerAlmaFootprint(Aladin aladin) {
        this.aladin = aladin;
        this.init();
        this.createChaine();

        this.buildGUI();
    }

    /** Initialisation des variables propres au footprint ALMA */
    protected void init() {
       type    = APPLI;
       aladinLabel   = "ALMA footprint";
       aladinLogo    = "ALMALogo.png";
    }

    protected void createChaine() {
        // TODO : localisation
        title = aladin.chaine.getString("ALTITLE");
        description = aladin.chaine.getString("ALINFO");
        super.createChaine();
     }

     private void buildGUI() {
         setBackground(Aladin.BLUE);
         setLayout(null);
         setFont(Aladin.PLAIN);

         int y =  50;
         int x = 150;

         // Le titre
         JPanel tp = new JPanel();
         Dimension d = makeTitle(tp,title);
         tp.setBackground(Aladin.BLUE);
         tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
         add(tp);

         // Un texte d'aide pour remplit le formulaire
         JLabel l = new JLabel(description);
         l.setBounds(90,y,400, 20); y+=20;
         add(l);
     }


     public void startFPGeneration(Point2D p, double width, double height, double radius) {
         Set<Point2D> centers = new TreeSet<Point2D>();
         for (int x=0; 2*radius*x<=width/2; x++) {
             for (int y=0; 2*radius*y<=height/2; y++) {
                 double curX = 2*x*radius;
                 if (y%2==1) curX += radius;
                 if (curX>width/2) {
                     continue;
                 }
                 double curY = 2*y*radius;
                 centers.add(new Point2D(curX, curY));
                 System.out.println(curX+ "\t" + curY);
             }
         }
     }

        static public class Point2D implements Comparable<Point2D> {
            private double x;
            private double y;

            public Point2D(double x, double y) {
                this.x = x;
                this.y = y;
            }

            public boolean equals(Object obj) {
                if (! (obj instanceof Point2D)) {
                    return false;
                }
                Point2D p = (Point2D)obj;
                return p.x==this.x && p.y==this.y;
            }

            public int compareTo(Point2D p) {
                double eps = 1;
                if (Math.abs(p.x-this.x)<eps && Math.abs(p.y-this.y)<eps) {
                    return 0;
                }
                return 1;
            }

        }

        public void test() {
            startFPGeneration(new Point2D(0, 0), 400, 200, 10);
        }


}
