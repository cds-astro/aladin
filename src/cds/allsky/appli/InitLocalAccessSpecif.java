// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//


package cds.allsky.appli;

import cds.allsky.BuilderIndex;

public class InitLocalAccessSpecif extends BuilderIndex {


	/**
	 * Lance l'initialisation de l'index healpix pour des besoins particuliers
	 * Le chemin de sortie est celui donn� en entr�e + ALLSKY + r�pertoire HpxFinder
	 * on peut passer une expression r�guli�re pour d�finir les fichiers � traiter
	 * (utlis�e via Pattern.matches)
	 * @param args chemin_entr�e regex order [chemin_sortie]
	 * @see #AllskyConst.HPX_FINDER
	 */
	public static void main(String[] args) {
		long t=System.currentTimeMillis();
		String pathSource = args[0]+ "/";
		String pathDest = pathSource;
		if (args.length>3)
			pathDest = args[3];
		String regex = args[1];
		int order =   Integer.parseInt(args[2]);

		BuilderIndex init = new BuilderIndex();
		System.out.println("using regex : "+regex);
		init.build(pathSource, pathDest, order, regex);
		System.out.println("done => "+(System.currentTimeMillis()-t)+"ms");
	}
}
