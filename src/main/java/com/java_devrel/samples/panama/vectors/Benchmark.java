package com.java_devrel.samples.panama.vectors;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 100, batchSize = 10)
@Measurement(iterations = 100_000)
@Fork(value = 1)
public class Benchmark {

    double[] a;
    double[] b;

    @Setup
    public void setup() {
        a = Vectors.supplier.get();
        b = Vectors.supplier.get();
    }


    @org.openjdk.jmh.annotations.Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void benchmarkVectorized() {
        Vectors.sumOf_vectorized(a, b);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void benchmarkForLoop() {
        Vectors.sumOf_forLoop(a, b);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void benchmarkStreamed() {
        Vectors.sumOf_streamed(a, b);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
