package com.openjdk.samples.panama.third_party.ffmpeg;

import static com.openjdk.samples.ffmpeg.libavformat.version.version_h.*;
import static com.openjdk.samples.ffmpeg.libavcoded.version.version_h.*;

public class Versions {

    public static void main(String[] args) {
        var libavformatMajor = LIBAVFORMAT_VERSION_MAJOR();
        var libavformatMinor = LIBAVFORMAT_VERSION_MINOR();
        var libavformatMicro = LIBAVFORMAT_VERSION_MICRO();

        var libavcodecMajor = LIBAVCODEC_VERSION_MAJOR();
        var libavcodecMinor = LIBAVCODEC_VERSION_MINOR();
        var libavcodecMicro = LIBAVCODEC_VERSION_MICRO();

        System.out.printf("libavformat version: %s.%s.%s\nlibavcodec version: %s.%s.%s\n",
                libavformatMajor, libavformatMinor, libavformatMicro,
                libavcodecMajor, libavcodecMinor, libavcodecMicro
        );
    }
}
