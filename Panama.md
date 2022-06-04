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
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar PrintfSimplified
```

### OpenJDK Panama Part 2

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar PrintfDefinedVariadic
```


```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar MethodTypeExample
```

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar PrintfUnpredictableVariadic
```

### OpenJDK Panama Part 3

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar Printf
```

### OpenJDK Panama Part 4

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar WalkDirectory
```
