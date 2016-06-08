#!/usr/bin/env bash
#  vim:ts=4:sts=4:sw=4:et
#
#  Author: Hari Sekhon
#  Date: 2016-01-26 23:36:03 +0000 (Tue, 26 Jan 2016)
#
#  https://github.com/harisekhon/nagios-plugin-kafka
#
#  License: see accompanying Hari Sekhon LICENSE file
#
#  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback
#
#  https://www.linkedin.com/in/harisekhon
#

set -euo pipefail
[ -n "${DEBUG:-}" ] && set -x

srcdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd "$srcdir/.."

. "$srcdir/utils.sh"

echo "
# ============================================================================ #
#                                   K a f k a
# ============================================================================ #
"

# TODO: latest container 2.11_0.10 doesn't work yet, no leader takes hold
#export KAFKA_VERSIONS="2.11_0.10 2.11_0.10 latest"
export KAFKA_VERSIONS="${@:-latest 2.10_0.8 2.11_0.8 2.10_0.9 2.11_0.9}"

KAFKA_HOST="${DOCKER_HOST:-${KAFKA_HOST:-${HOST:-localhost}}}"
KAFKA_HOST="${KAFKA_HOST##*/}"
KAFKA_HOST="${KAFKA_HOST%%:*}"
export KAFKA_HOST

export ZOOKEEPER_PORT="${ZOOKEEPER_PORT:-2181}"
export KAFKA_PORT="${KAFKA_PORT:-9092}"

export DOCKER_IMAGE="harisekhon/kafka"
export DOCKER_CONTAINER="nagios-plugin-kafka-test"

export KAFKA_TOPIC="nagios-plugin-kafka-test"

# needs to be longer than 10 to allow Kafka to settle so topic creation works
startupwait=20

test_kafka(){
    local version="$1"
    travis_sample || continue
    echo "Setting up Apache Kafka $version test container"
    hr
    launch_container "$DOCKER_IMAGE:$version" "$DOCKER_CONTAINER" $KAFKA_PORT $ZOOKEEPER_PORT
    hr
    echo "creating Kafka test topic"
    docker exec -ti "$DOCKER_CONTAINER" kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic "$KAFKA_TOPIC" || :

    hr
    scala target/scala-*/check_kafka-assembly-*.jar com.linkedin.harisekhon.CheckKafka -H $HOST -P $KAFKA_PORT
    hr
    delete_container
    echo
}

for version in $KAFKA_VERSIONS; do
    test_kafka $version
done
