FROM ibmjava:8-sdk

ENV target /usr/src/pubsub-worker
WORKDIR $target

COPY ./gradle $target/gradle
COPY ./gradlew $target
COPY ./build.gradle $target

RUN ./gradlew downloadDependencies

COPY . $target/
RUN ./gradlew build

ENTRYPOINT ["java", "-jar", "build/libs/pubsub-worker-2.0.jar"]

