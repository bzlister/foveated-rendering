import java.util.LinkedList;
import java.util.ListIterator;

public class SimpleTemporalCompression implements TemporalCompression {

    private int THRESH_L;
    private int THRESH_H;

    public SimpleTemporalCompression(int THRESH_L, int THRESH_H){
        this.THRESH_L = THRESH_L;
        this.THRESH_H = THRESH_H;
    }

    @Override
    public LinkedList<Integer> intraFrameEncode(byte[] frame, byte[] previous, LinkedList<Integer> referencePoints, int gazeX, int gazeY, double r, int w) {
        int threshold = -1;
        ListIterator<Integer> iter = referencePoints.listIterator();
        while (iter.hasNext()){
            boolean change = false;
            int x = iter.next();
            if (Math.pow((x/3)%w - gazeX, 2) + Math.pow((x/3)/w - gazeY, 2) < r*r){
                threshold = THRESH_H;
            }
            else {
                threshold = THRESH_L;
            }
            if (Math.abs((int) previous[x - 2] - (int) frame[x - 2]) > threshold) {
                change = true;
            }
            if (Math.abs((int) previous[x - 1] - (int) frame[x - 1]) > threshold) {
                change = true;
            }
            if (Math.abs((int) previous[x] - (int) frame[x]) > threshold) {
                change = true;
            }
            if (!change && ((x/3)%w != 2)){
                iter.remove();
            }
        }
        return referencePoints;
    }
}
