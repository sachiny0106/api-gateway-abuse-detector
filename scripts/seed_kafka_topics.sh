#!/bin/bash

echo "Creating Kafka topics..."

KAFKA_BROKER="localhost:9092"

docker exec abuse-detector-kafka kafka-topics \
  --create \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 6 \
  --replication-factor 1 \
  --topic request-events

docker exec abuse-detector-kafka kafka-topics \
  --create \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 2 \
  --replication-factor 1 \
  --topic decision-events

echo "Topics created:"
docker exec abuse-detector-kafka kafka-topics \
  --list \
  --bootstrap-server $KAFKA_BROKER