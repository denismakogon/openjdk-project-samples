package com.openjdk.samples.panama.third_party.jextract_jar_jmod;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class CLI {

    private static final String JEXTRACT = "jextract";
    private static final String javaHome = System.getProperty("java.home", "/usr/local/openjdk-19");
    private static final String javaHomeBinDir = javaHome + "/bin";
    private static final String jextractBinary = javaHomeBinDir + "/" + JEXTRACT;
    private static final File stdIn = new File("/dev/stdin");
    private static final File stdOut = new File("/dev/stdout");
    private static final File stdErr = new File("/dev/stderr");

    public static void main(String[] args) throws IOException, InterruptedException {
        var jextractProcess = new ProcessBuilder()
                .inheritIO()
                .redirectErrorStream(true)
                .redirectInput(stdIn)
                .redirectOutput(stdOut)
                .redirectError(stdErr)
                .command(List.of(
                    jextractBinary, "-h"
        ));
        var proc = jextractProcess.start();
        System.out.println(proc.waitFor());
    }

}
