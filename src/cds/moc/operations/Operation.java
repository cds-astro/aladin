package cds.moc.operations;

import healpix.core.HealpixIndex;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import cds.moc.HealpixMoc;

/**
*
* @author Thomas Boch
*
*/

public abstract class Operation {
    static private boolean DEBUG = false;

    private HealpixMoc moc1, moc2;
    private int nside;

    public Operation(HealpixMoc moc1, HealpixMoc moc2, int nside) {
        this.moc1 = moc1;
        this.moc2 = moc2;
        this.nside = nside;
    }

    public abstract HealpixMoc compute();

    static protected void fillArray(HealpixMoc moc, boolean[] pix, int nside) {
        Iterator<long[]> it = moc.iterator();
        int order = HealpixIndex.nside2order(nside);
        long[] d;
        int curOrder;
        long curIpix;
        int ipixStart, ipixEnd;
        int orderDiff;
        while (it.hasNext()) {
            d = it.next();
            curOrder = (int)d[0];
            curIpix = d[1];
            orderDiff = order-curOrder;
            if (orderDiff==0) {
                pix[(int)curIpix] = true;
            }
            else if (orderDiff<0) {
                pix[(int) (curIpix/Math.pow(4., -orderDiff))] = true;
            }
            else if (orderDiff>0) {
                ipixStart = (int) (curIpix*Math.pow(4., orderDiff));
                ipixEnd = (int) (ipixStart + Math.pow(4., orderDiff) -1 );

                for (int ipix=ipixStart; ipix<=ipixEnd; ipix++) {
                    pix[ipix] = true;
                }
            }
        }
    }

    static protected int getNbTrue(boolean[] bArray) {
        int count = 0;
        for (boolean b : bArray) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    static protected ArrayList<Long> getTrueIndexes(boolean[] bArray) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (int i = 0; i < bArray.length; i++) {
            boolean b = bArray[i];
            if (b) {
                l.add((long)i);
            }
        }

        return l;
    }

    protected void processFor(HealpixMoc moc, int nside, boolean[] cells) {
        int upperLevelNside = nside/2;

        int nbTrue = getNbTrue(cells);

        if (nbTrue==0) {
            return;
        }

        if (nside==2) {
            if (DEBUG) {
                System.out.println("\nNSIDE: "+nside);
                System.out.println("nb cells: "+nbTrue+"/"+HealpixIndex.nside2Npix(nside)+"\n\n");
                System.out.println("\n\n");
            }

            int order = HealpixIndex.nside2order(nside);
            ArrayList<Long> indexes = getTrueIndexes(cells);
            for (Long index : indexes) {
                moc.add(order, index);
            }
            return;
        }


        int nbCells = (int)HealpixIndex.nside2Npix(upperLevelNside);
        boolean[] cellsUpperLevel = new boolean[nbCells];
        for (int i = 0; i < cellsUpperLevel.length; i++) {
            cellsUpperLevel[i] = false;
        }

        int nbRemoved = 0;
        for (int ipix = 0; ipix < nbCells; ipix++) {
            if (DEBUG && ipix%100==0) {
                if (ipix%8000==0) {
                    System.out.print("\n");
                }
                System.out.print(".");
            }

            //
            int[] children = new int[4];
            for (int i=0; i<4; i++) {
                children[i] = ipix*4+i;
            }
            if (cells[children[0]] && cells[children[1]] && cells[children[2]] && cells[children[3]] ) {
                cells[children[0]] = cells[children[1]] = cells[children[2]] = cells[children[3]] = false;
                nbRemoved += 4;
                cellsUpperLevel[ipix] = true;
            }

        }

        int order = HealpixIndex.nside2order(nside);
        ArrayList<Long> indexes = getTrueIndexes(cells);
        for (Long index : indexes) {
            moc.add(order, index);
        }

        if (DEBUG) {
            System.out.println("\nNSIDE: "+nside);
            System.out.println("nb cells: "+getNbTrue(cells)+"/"+HealpixIndex.nside2Npix(nside));
        }

        processFor(moc, upperLevelNside, cellsUpperLevel);

    }


    public HealpixMoc getMoc1() {
        return moc1;
    }

    public void setMoc1(HealpixMoc moc1) {
        this.moc1 = moc1;
    }

    public HealpixMoc getMoc2() {
        return moc2;
    }

    public void setMoc2(HealpixMoc moc2) {
        this.moc2 = moc2;
    }

    public int getNside() {
        return nside;
    }

    public void setNside(int nside) {
        this.nside = nside;
    }

    public static void main(String[] args) {
        DEBUG = Boolean.parseBoolean(System.getProperty("cds.debug"));

        double start = System.currentTimeMillis();

        if (args.length<6) {
            System.out.println("Usage:\n" +
                    "java -jar FootprintOperation.jar <MOC-file1> <MOC-file2> <result-file> [intersection|union] <nside> [ascii|fits]");
            System.exit(0);
        }

        File f1 = new File(args[0]);
        if ( ! f1.exists()) {
            System.err.println("Unknown file "+f1+", exiting !");
            System.exit(1);
        }
        File f2 = new File(args[1]);
        if ( ! f2.exists()) {
            System.err.println("Unknown file "+f2+", exiting !");
            System.exit(1);
        }

        String resultPath = args[2];

        String[] allowedOperations = {"union", "intersection", "substraction"};
        String operation = args[3].toLowerCase();
        if ( ! Arrays.asList(allowedOperations).contains(operation)) {
            System.err.println("Unknown operation "+operation+". Allowed operations are intersection|union");
            System.exit(1);
        }

        int nside = 1;
        try {
            nside = Integer.parseInt(args[4]);
            if (Integer.bitCount(nside)>1) {
                System.err.println("nside must be a power of 2, exiting !");
                System.exit(1);
            }
        }
        catch(NumberFormatException nfe) {
            System.err.println("Invalid value "+args[4]+" for nside");
            System.exit(1);
        }

        String format = args[5].toLowerCase();
        if ( !format.equals("ascii") && !format.equals("fits")) {
            System.err.println("Unknown format "+format+". Allowed formats are fits|ascii");
        }
        boolean ascii = format.equals("ascii");

        HealpixMoc moc1 = new HealpixMoc();
        try {
            moc1.read(f1.getAbsolutePath(), ascii?HealpixMoc.ASCII:HealpixMoc.FITS);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.err.println("Error while reading MOC1 file, exiting !");
            System.exit(1);
        }

        HealpixMoc moc2 = new HealpixMoc();
        try {
            moc2.read(f2.getAbsolutePath(), ascii?HealpixMoc.ASCII:HealpixMoc.FITS);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.err.println("Error while reading MOC2 file, exiting !");
            System.exit(1);
        }

        Operation op = null;
        if (operation.equals("intersection")) {
            op = new Intersection(moc1, moc2, nside);
        }
        else if (operation.equals("union")) {
            op = new Union(moc1, moc2, nside);
        }

        HealpixMoc result = op.compute();
        try {
            result.write(resultPath, ascii?HealpixMoc.ASCII:HealpixMoc.FITS);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.err.println("Error while writing result file !");
            System.exit(1);
        }

        double end = System.currentTimeMillis();

        if (DEBUG) {
            System.out.println("\nTotal time to compute operation: "+(end-start+"ms"));
        }
    }

}
