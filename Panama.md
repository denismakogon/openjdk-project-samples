# OpenJDK Panama

## How to start?

1. Install the JDK.
2. Install Maven.
3. Clone this project.

## Build the project

```shell
mvn clean package
```

## Run it!

### OpenJDK Panama Part 1
```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_1.PrintfSimplified
```

### OpenJDK Panama Part 2

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_2.FunctionDescriptorToMethodType
```

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_2.PrintfDefinedVariadic
```

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_2.MethodTypeExample
```

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_2.PrintfUnpredictableVariadic
```

### OpenJDK Panama Part 3

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_3.Printf
```

### OpenJDK Panama Part 4

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.panama.part_4.WalkDirectory
```

### OpenJDK Panama Part 5

Please see [https://github.com/denismakogon/cpp-to-c-api](https://github.com/denismakogon/cpp-to-c-api) before running the application.

```shell
DEBUG=1 java --enable-native-access=ALL-UNNAMED \
  --enable-preview -cp target/openjdk-samples-1.0.jar \
  com.java_devrel.samples.panama.part_5.Main
```
