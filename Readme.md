# GDAX Kafka Soure

I made a python implementation for this exact problem earlier. But I was not
happy with the performance of it and decided to learn kotlin and refresh
my JVM skills.

## Issues
There are no tests, any opinions for what should be tested and how is much
appreciated. Logging verbosity is not configured again same as with logging.

There is also no proper failure management. I've not decided what level of
consistency failure recovery this should be built for, as we the more
interesting usecases essentially do CDC from the order books. Proper recovery
when there is an outage in the stream requires a new snapshot to acquired after
the stream as been restarted. This is not an issue for the `level2` channel as
it always starts with such a message, however for full this is neccessary.

For kafka outages I need to read more about the asynchronous producer mode that
is used, it seems it has some built in buffer limitations and could actually
transparently solve the reconnection at the cheapest possible cost of increased
delivery latency.

## Running - Docker
The following compose file works decently, it is not considered supported
or endorsed. If you have a need to run this service you probably know what
to do.

```
version: '2.1'
services:

  zookeeper:
    image: confluentinc/cp-zookeeper
    hostname: zookeeper
    ports:
      - '32181:32181'
    environment:
      ZOOKEEPER_CLIENT_PORT: 32181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka
    hostname: kafka
    ports:
      - '9092:9092'
      - '29092:29092'
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:32181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092

  gdax_source:
    build: .
    depend_on:
      - kafka
    environment:
      GDAX_KAFKA_SERVERS: kafka:29092
      GDAX_KAFKA_TOPIC: gdax
      GDAX_PRODUCTS: "BTC-USD, ETH-USD, BTC-EUR, ETH-EUR"
      GDAX_CHANNELS: ticker
```

## Running - Jar
There is a gradle task to build a fat jar. Not sure if this a great idea.


## Conclusion
Not done with this, I'll probably end up running one for all tickers permanently
for data capture and the `level2` or `full` for capturing data for interesting
periods.

The performance is excellent compared to the old python implementation, on my
laptop it never saturates a core for any reasonable usecase. Capturing `ETH-USD`
and `BTC-USD` in the `full` channel ends up at 5%~ average load.
