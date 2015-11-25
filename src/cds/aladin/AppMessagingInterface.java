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

package cds.aladin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// interface pour PLASTIC/SAMP
public interface AppMessagingInterface {

    // les différents états possibles
    static final int NO_PLASTIC = 0;
    static final int PLASTIC_NOT_CONNECTED = 1;
    static final int PLASTIC_CONNECTED_ALONE = 2;
    static final int PLASTIC_CAN_TRANSMIT = 3;

    static final AbstractMessage ABSTRACT_MSG_LOAD_FITS = new AbstractMessage();
    static final AbstractMessage ABSTRACT_MSG_LOAD_VOT_FROM_URL = new AbstractMessage();
    static final AbstractMessage ABSTRACT_MSG_LOAD_SPECTRUM_FROM_URL = new AbstractMessage();
    static final AbstractMessage ABSTRACT_MSG_LOAD_CHARAC_FROM_URL = new AbstractMessage();

    // icon URL
    static final public String ICON_URL = "http://aladin.u-strasbg.fr/aladin_large.gif";

//    public abstract Object getMessage(AbstractMessage abstractMessage);

    public abstract boolean broadcastImage(final Plan planImage, final String[] recipients);

    public abstract boolean broadcastTable(final Plan planCatalog, final String[] recipients);

    public abstract ArrayList<String> getAppsSupportingTables();

    public abstract ArrayList<String> getAppsSupporting(AbstractMessage message);

    public abstract Object getAppWithName(String s);

    public abstract boolean getPlasticTrace();

    public abstract void setPlasticTrace(boolean plasticTrace);

    public abstract PlasticWidget getPlasticWidget();

    public abstract boolean isRegistered();

    public abstract boolean internalHubRunning();

    public abstract void updateState();

    public abstract boolean register(boolean silent, boolean launchHubIfNeeded);

//    public abstract void sendAsyncMessage(URI message, List args, List recipients);

    // TODO : homogenize method names and arguments list

    /**
     * si recipients==null --> broadcast
     */
    public abstract void sendMessageLoadSpectrum(String url, String spectrumId, String spectrumName, Map metadata, List recipients);

    public abstract void sendMessageLoadCharac(String url, String name, List recipients);

    public abstract void sendMessageLoadImage(String url, String name, List recipients);

    public abstract void setPlasticWidget(PlasticWidget widget);

    public abstract void sendHighlightObjectsMsg(Source source);

    public abstract void sendSelectObjectsMsg();

    public abstract boolean startInternalHub();

    public abstract void stopInternalHub(boolean dontAsk);

    public abstract void trace(String s);

    public abstract boolean unregister();

    public abstract boolean unregister(boolean force);

    public abstract boolean unregister(boolean force, boolean destroyInternalHub);

    public abstract void pointAtCoords(double ra, double dec);

    public abstract String getProtocolName();

    public abstract boolean ping();

    static public class AbstractMessage {}

}
