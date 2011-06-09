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

/**
 *
 * @author boch
 *
 */
public class PlaneLoadEvent {
    static public int SUCCESS = 0;
    static public int ERROR = 1;

    public Plan plane;
    public int status;
    public String errorMsg;

    public PlaneLoadEvent(Plan plane, int status, String errorMsg) {
        this.plane = plane;
        this.status = status;
        this.errorMsg = errorMsg;
    }
}
