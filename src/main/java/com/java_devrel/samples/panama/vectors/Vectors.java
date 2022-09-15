package com.java_devrel.samples.panama.vectors;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class Vectors {


    record evolving(long[] vectorized, long[] streamed, long[] forLoop) {}

    static Random r = new Random();
    static int vectorSize = 1_000_000;
    public static Supplier<double[]> supplier = () -> IntStream.range(0, vectorSize)
            .mapToDouble(i -> i * r.nextDouble()).toArray();

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    static double[] sumOf_vectorized(double[] a, double[] b) {
        var operandSize = a.length;
        var result = new double[operandSize];
        var upperBound = SPECIES.loopBound(operandSize);

        IntStream.iterate(0, i -> i < upperBound, i -> i + SPECIES.length()).forEachOrdered(i -> {
            var va = DoubleVector.fromArray(SPECIES, a, i);
            var vb = DoubleVector.fromArray(SPECIES, b, i);
            var vc = va.add(vb);
            vc.intoArray(result, i);
        });
        for (int i = upperBound; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    static double[] sumOf_forLoop(double[] a, double[] b) {
        var operandSize = a.length;
        var result = new double[operandSize];
        for(int i = 0; i < operandSize; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    static double[] sumOf_streamed(double[] a, double[] b) {
        return IntStream.range(0, a.length).mapToDouble(index -> a[index] + b[index]).toArray();
    }

    static evolving test(int tries) {
        System.out.printf("Number of tries: %d\n", tries);
        var vectorSize = 1_000_000;
        var r = new Random();
        Supplier<double[]> supplier = () -> IntStream.range(0, vectorSize)
                .mapToDouble(i -> i * r.nextDouble()).toArray();

        var vectorized = IntStream.range(0, tries).mapToLong(index -> {
            var a = supplier.get();
            var b = supplier.get();
            var now = Instant.now();
            sumOf_vectorized(a, b);
            var later = Instant.now();
            return Duration.between(now, later).toMillis();
        }).toArray();
        var vectorizedAverage = Arrays.stream(vectorized).average().getAsDouble();
        System.out.printf("Average vectorized sum took %f milliseconds.\n", vectorizedAverage);

        var streamed = IntStream.range(0, tries).mapToLong(index -> {
            var a = supplier.get();
            var b = supplier.get();
            var now = Instant.now();
            sumOf_streamed(a, b);
            var later = Instant.now();
            return Duration.between(now, later).toMillis();
        }).toArray();
        var streamedAverage = Arrays.stream(streamed).average().getAsDouble();
        System.out.printf("Average stream-bound sum took %f milliseconds.\n", streamedAverage);

        var forLoop = IntStream.range(0, tries).mapToLong(index -> {
            var a = supplier.get();
            var b = supplier.get();
            var now = Instant.now();
            sumOf_streamed(a, b);
            var later = Instant.now();
            return Duration.between(now, later).toMillis();
        }).toArray();
        var forLoopAverage = Arrays.stream(forLoop).average().getAsDouble();
        System.out.printf("Average for-loop-bound sum took %f milliseconds.\n", forLoopAverage);

        return new evolving(vectorized, streamed, forLoop);
    }


    public static void main(String... args) {
        var result = test(30_000);
    }

}