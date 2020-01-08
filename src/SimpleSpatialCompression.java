import java.util.LinkedList;

public class SimpleSpatialCompression implements SpatialCompression {

    private int THRESH_L;
    private int THRESH_H;

    public SimpleSpatialCompression(int THRESH_L, int THRESH_H){
        this.THRESH_L = THRESH_L;
        this.THRESH_H = THRESH_H;
    }

    @Override
    public LinkedList<Integer> interFrameEncode(byte[] frame, int gazeX, int gazeY, double r, int w) {
        LinkedList<Integer> referencePoints = new LinkedList<Integer>();
        int thresh = THRESH_L;
        int[] reference = new int[]{(int)frame[0], (int)frame[1], (int)frame[2]};
        referencePoints.add(2);
        for (int x = 5; x < frame.length; x+=3){
            if (Math.pow((x/3)%w - gazeX, 2) + Math.pow((x/3)/w - gazeY, 2) < r*r){ // In-focus region
                thresh = THRESH_H;
            }
            else {
                thresh = THRESH_L;
            }
            if ((x/3)%w ==2
                    ||((Math.abs(reference[0] - (int) frame[x - 2]) > thresh)
                    ||(Math.abs(reference[1] - (int) frame[x - 1]) > thresh)
                    ||(Math.abs(reference[2] - (int) frame[x]) > thresh))){
                referencePoints.add(x);
                reference = new int[]{(int) frame[x-2], (int)frame[x-1], (int)frame[x]};
            }
        }
        return referencePoints;
    }
}
