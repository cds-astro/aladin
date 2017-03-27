package cds.aladin;

public interface GrabItFrame {

	/**
	 * Mise en place du target en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	void setGrabItCoord(double x, double y);

	/**
	 * Arrete le GrabIt
	 */
	void stopGrabIt();

	/**
	 * Retourne true si le bouton grabit du formulaire existe et qu'il est
	 * enfoncé
	 */
	boolean isGrabIt();

	/**
	 * Mise en place du radius en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	void setGrabItRadius(double x1, double y1, double x2, double y2);

}
