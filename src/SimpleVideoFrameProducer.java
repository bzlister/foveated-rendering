import org.jcodec.api.FrameGrab;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.Yuv420jToRgb;

import java.io.IOException;
import java.util.LinkedList;

public class SimpleVideoFrameProducer extends VideoFrameProducer {

    private Yuv420jToRgb converter = new Yuv420jToRgb();
    private byte[] previous;
    private boolean first = true;

    public SimpleVideoFrameProducer(SpatialCompression spatialCompression, TemporalCompression temporalCompression) {
        super(spatialCompression, temporalCompression);
    }

    @Override
    public void init(FrameGrab frameGrab, int frameWidth, int frameHeight){
        super.init(frameGrab, frameWidth, frameHeight);
        previous = new byte[w*h*3];
    }

    @Override
    public byte[] getNextFrame(int gazeX, int gazeY, double r) throws EndOfVideoException, IOException {
        Picture yuv = frameGrab.getNativeFrame();
        if (yuv == null){
            throw new EndOfVideoException();
        }
        Picture rgb = Picture.create(w, h, ColorSpace.RGB);
        converter.transform(yuv, rgb);
        byte[] frame = rgb.getData()[0];
        LinkedList<Integer> referencePoints = spatialCompression.interFrameEncode(frame, gazeX, gazeY, r, w);
        if (first){
            first = false;
            previous = frame;
            return frame;
        }
        referencePoints = temporalCompression.intraFrameEncode(frame, previous, referencePoints, gazeX, gazeY, r, w);
        byte[] toBeSent = new byte[6*referencePoints.size()];
        int old = 2;
        int i = 0;
        byte[] oldVals = new byte[]{frame[0], frame[1], frame[2]};
        for (Integer x : referencePoints) {
            while (old < x) {
                previous[old - 2] = oldVals[0];
                previous[old - 1] = oldVals[1];
                previous[old] = oldVals[2];
                old+=3;
            }
            previous[x - 2] = frame[x - 2];
            previous[x - 1] = frame[x - 1];
            previous[x] = frame[x];
            toBeSent[i] = (byte) ((x & 0x00FF0000) >> 16);
            toBeSent[i + 1] = (byte) ((x & 0x0000FF00) >> 8);
            toBeSent[i + 2] = (byte) ((x & 0x000000FF) >> 0);
            toBeSent[i + 3] = frame[x - 2];
            toBeSent[i + 4] = frame[x - 1];
            toBeSent[i + 5] = frame[x];
            i += 6;
            old = x + 2;
        }
        System.out.println((1.0*toBeSent.length)/frame.length);
        return toBeSent;
    }
}
