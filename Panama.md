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

### stdlib: stdio

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.openjdk.samples.panama.stdlib.Printf
```

### ffmpeg

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.openjdk.samples.panama.ffmpeg.Versions
```

```shell
java --enable-native-access=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.openjdk.samples.panama.ffmpeg.FilesWalk`
```
