// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.aladin;

import java.awt.*;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JPanel;

import cds.tools.Util;

/**
 * Gestion de la fenetre associee a la creation d'un plan arithmetique pour les
 * plans Healpix
 *
 * Code largement repris de FrameArithmetic TODO : faut-il fusionner les 2, et n'avoir qu'une fenetre unique ?
 *
 * @author Thomas Boch [CDS]
 * @version 0.9 : (may 2010) Creation
 */
public final class FrameHealpixArithmetic extends FrameRGBBlink {

    String TITLE, INFO, HELP1, ADD, SUB, MUL, DIV, PLANE, PLANEVALUE;

    private ButtonGroup cbg; // Les checkBox des opérations possibles

    @Override
    protected void createChaine() {
        super.createChaine();
        TITLE = a.chaine.getString("HARITHTITLE");
        INFO = a.chaine.getString("HARITHINFO");
        HELP1 = a.chaine.getString("HARITHHELP");
        ADD = a.chaine.getString("ARITHADD");
        SUB = a.chaine.getString("ARITHSUB");
        MUL = a.chaine.getString("ARITHMUL");
        DIV = a.chaine.getString("ARITHDIV");
        PLANE = a.chaine.getString("ARITHPLANE");
        PLANEVALUE = a.chaine.getString("ARITHPLANEVALUE");
    }

    /** Creation du Frame */
    protected FrameHealpixArithmetic(Aladin aladin) {
        super(aladin);
        Aladin.setIcon(this);
    }

    @Override
    protected String getTitre() {
        return TITLE;
    }

    @Override
    protected String getInformation() {
        return INFO;
    }

    @Override
    protected String getHelp() {
        return HELP1;
    }

    @Override
    protected int getToolNumber() {
        return -2;
    }

    @Override
    protected int getNb() {
        return 2;
    }

    @Override
    protected String getLabelSelector(int i) {
        return i == 0 ? PLANE : PLANEVALUE;
    }

    /** Recupere la liste des plans images valides */
    @Override
    protected PlanHealpix[] getPlan() {
        Vector<Plan> v = a.calque.getPlans(PlanHealpix.class);
        if (v == null) {
            return new PlanHealpix[0];
        }
        PlanHealpix pi[] = new PlanHealpix[v.size()];
        v.copyInto(pi);
        return pi;
    }

    @Override
    protected Color getColorLabel(int i) {
        return Color.black;
    }

    @Override
    protected JPanel getAddPanel() {
        GridBagConstraints c = new GridBagConstraints();
        GridBagLayout g = new GridBagLayout();
        c.fill = GridBagConstraints.BOTH;

        JPanel p = new JPanel();
        p.setLayout(g);

        cbg = new ButtonGroup();

        JPanel pp = new JPanel();
        JRadioButton cb;
        cb = new JRadioButton(ADD);
        cb.setActionCommand(ADD);
        cbg.add(cb);
        pp.add(cb);
        cb.setSelected(true);
        cb = new JRadioButton(SUB);
        cb.setActionCommand(SUB);
        cbg.add(cb);
        pp.add(cb);
        cb = new JRadioButton(MUL);
        cb.setActionCommand(MUL);
        cbg.add(cb);
        pp.add(cb);
        cb = new JRadioButton(DIV);
        cb.setActionCommand(DIV);
        cbg.add(cb);
        pp.add(cb);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 10.0;
        g.setConstraints(pp, c);
        p.add(pp);

        return p;
    }

    private int getOperation(String s) {
        if (s.equals(ADD))
            return PlanImageAlgo.ADD;
        if (s.equals(SUB))
            return PlanImageAlgo.SUB;
        if (s.equals(MUL))
            return PlanImageAlgo.MUL;
        return PlanImageAlgo.DIV;
    }

    @Override
    protected void submit() {
        try {
            PlanHealpix p1 = (PlanHealpix) getPlan(ch[0]), p2 = (PlanHealpix) getPlan(ch[1]);

            while (p1 != null && !p1.isSync() || p2 != null && !p2.isSync()) {
                Util.pause(500);
            }

            double coef = 0;
            if (p2 == null) {
                coef = Double.parseDouble(((String) ch[1].getSelectedItem()).trim());
            }

            String s = cbg.getSelection().getActionCommand();
            int fct = getOperation(s);
             a.calque.newPlanHealpixAlgo(s.substring(0,3),p1,p2,fct,coef);
            hide();

        } catch (Exception e) {
            if (a.levelTrace>=3) {
                e.printStackTrace();
            }
            Aladin.error("Healpix arithmetic operation failed !");
        }

    }

    @Override
    protected void adjustWidgets() {
        ch[1].setEditable(true);
    };
}
