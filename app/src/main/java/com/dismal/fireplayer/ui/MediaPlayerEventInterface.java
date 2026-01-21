package com.dismal.fireplayer.ui;

public class MediaPlayerEventInterface {

    public interface MeidaPlayerEventListener {
        void onCompletion();

        void onMediaPlayerError();

        void onNewSource(String str);
    }
}
