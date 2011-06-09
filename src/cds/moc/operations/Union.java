package cds.moc.operations;

import cds.moc.HealpixMoc;

/**
*
* @author Thomas Boch
*
*/

public class Union extends Operation {

    public Union(HealpixMoc moc1, HealpixMoc moc2, int nside) {
        super(moc1, moc2, nside);
    }

    public HealpixMoc compute() {
        HealpixMoc result = new HealpixMoc();
        int nbPix = 12*getNside()*getNside();
        boolean[] pixM1 = new boolean[nbPix];
        boolean[] pixM2 = new boolean[nbPix];

        fillArray(getMoc1(), pixM1, getNside());
        fillArray(getMoc2(), pixM2, getNside());

        boolean[] pixResult = new boolean[nbPix];

        for (int i=0; i<pixResult.length; i++) {
            pixResult[i] = pixM1[i] || pixM2[i];
        }

        processFor(result, getNside(), pixResult);

        return result;
    }




    // for testing purposes
    public static void main(String[] args) {
        try {
            HealpixMoc m1 = new HealpixMoc();
            m1.read("/data/boch/tmp/sdss.txt", HealpixMoc.ASCII);

            HealpixMoc m2 = new HealpixMoc();
            m2.read("/data/boch/tmp/denis.txt", HealpixMoc.ASCII);

            HealpixMoc result = new Union(m1, m2, 512).compute();
            result.write("/data/boch/tmp/result.txt", HealpixMoc.ASCII);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
