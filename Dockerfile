FROM java:openjdk-8-jdk-alpine

RUN mkdir /app
ADD . /app

WORKDIR /app
RUN ./gradlew build

CMD java -jar build/libs/*fat*.jar
