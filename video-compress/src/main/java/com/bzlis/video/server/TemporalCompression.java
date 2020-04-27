package com.bzlis.video.server;

import java.util.LinkedList;

public interface TemporalCompression {
    LinkedList<Integer> intraFrameEncode(byte[] frame, byte[] previous, LinkedList<Integer> referencePoints, int gazeX, int gazeY, double r, int w);
}
