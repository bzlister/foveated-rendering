package com.bzlis.video.server;

import org.jcodec.api.FrameGrab;

import java.io.IOException;

public abstract class VideoFrameProducer {

    protected SpatialCompression spatialCompression;
    protected TemporalCompression temporalCompression;
    protected FrameGrab frameGrab;
    protected int w;
    protected int h;

    public VideoFrameProducer(SpatialCompression spatialCompression, TemporalCompression temporalCompression){
        this.spatialCompression = spatialCompression;
        this.temporalCompression = temporalCompression;
    }

    public void init(FrameGrab frameGrab, int frameWidth, int frameHeight){
        this.frameGrab = frameGrab;
        w = frameWidth;
        h = frameHeight;
    }

    abstract byte[] getNextFrame(int gazeX, int gazeY, double r) throws EndOfVideoException, IOException;
}
