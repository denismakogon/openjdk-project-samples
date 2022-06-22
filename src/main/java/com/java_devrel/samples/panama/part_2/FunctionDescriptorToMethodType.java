package com.java_devrel.samples.panama.part_2;

import java.lang.foreign.*;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class FunctionDescriptorToMethodType {

    public static void main(String[] args) {
        var emptyDescriptor = FunctionDescriptor.of(JAVA_INT);
        System.out.println(Linker.downcallType(emptyDescriptor));

        var descriptorWithNamedArg = emptyDescriptor.appendArgumentLayouts(ADDRESS);
        System.out.println(Linker.downcallType(descriptorWithNamedArg));

        var descriptorWithNamedAndVariadicArg = descriptorWithNamedArg
                .asVariadic(ADDRESS, JAVA_INT);
        System.out.println(Linker.downcallType(descriptorWithNamedAndVariadicArg));
    }
}
