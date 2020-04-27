package com.bzlis.video.server;

import java.util.LinkedList;

public interface SpatialCompression {
    LinkedList<Integer> interFrameEncode(byte[] frame, int gazeX, int gazeY, double r, int w);
}
