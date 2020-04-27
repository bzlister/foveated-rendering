package com.bzlis.video.server;

public class EndOfVideoException extends Exception {
    @Override
    public String getMessage(){
        return "End of video";
    }
}
