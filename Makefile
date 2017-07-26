#  vim:ts=4:sts=4:sw=4:noet
#
#  Author: Hari Sekhon
#  Date: 2016-06-06 22:57:08 +0100 (Mon, 06 Jun 2016)
#
#  https://github.com/harisekhon/nagios-plugin-kafka
#
#  License: see accompanying Hari Sekhon LICENSE file
#
#  If you're using my code you're welcome to connect with me on LinkedIn
#  and optionally send me feedback to help improve or steer this or other code I publish
#
#  https://www.linkedin.com/in/harisekhon
#

SHELL=/bin/bash

ARGS=localhost:9092 test

.PHONY: build
build:
	make gradle

# used by CI
.PHONY: random-build
random-build:
	@# SBT + Maven Surefire plugin both get buffer overflow on openjdk7 :-/
	@x=$$(bash-tools/random_select.sh build mvn gradle sbt); echo make $$x; make $$x

.PHONY: mvn
mvn:
	make lib-mvn
	./mvnw clean package
	ln -sfv target/check_kafka-*.jar check_kafka.jar

.PHONY: gradle
gradle:
	make lib-gradle
	./gradlew clean shadowJar
	ln -sfv build/libs/check_kafka-*.jar check_kafka.jar

.PHONY: sbt
sbt:
	make lib-sbt
	sbt clean assembly
	ln -sfv target/scala-*/check_kafka-assembly-*.jar check_kafka.jar

# for testing
.PHONY: all
all:
	make mvn
	make gradle
	make sbt

.PHONY: lib-mvn
lib-mvn:
	make lib-update
	cd lib && make mvn

.PHONY: lib-gradle
lib-gradle:
	make lib-update
	cd lib && make gradle

.PHONY: lib-sbt
lib-sbt:
	make lib-update
	cd lib && make sbt
	sbt eclipse || echo "Ignore this last error, you simply don't have the SBT eclipse plugin, it's optional"

.PHONY: clean
clean:
	cd lib && make clean
	./mvnw clean || :
	sbt clean || :
	./gradlew clean || :
	rm -f check_kafka.jar

.PHONY: deep-clean
deep-clean:
	cd lib && make deep-clean
	make clean
	rm -rf .gradle ~/.gradle/{caches,native,wrapper} ~/.m2/{repository,wrapper} ~/.ivy2 ~/.sbt/boot

.PHONY: update
update:
	git pull
	make lib-update
	make

.PHONY: update2
update2:
	make update-no-recompile

.PHONY: update-no-recompile
update-no-recompile:
	git pull
	git submodule update --init --recursive

.PHONY: lib-update
lib-update:
	git submodule update --init

.PHONY: update-submodules
update-submodules:
	git submodule update --init --remote
.PHONY: updatem
updatem:
	make update-submodules

# useful for quicker compile testing
.PHONY: p
p:
	make package
.PHONY: package
package:
	make lib
	sbt package

.PHONY: sonar
sonar:
	make gradle-sonar

.PHONY: gradle-sonar
gradle-sonar:
	@# calls compileJava
	./gradlew sonarqube

.PHONY: mvn-sonar
mvn-sonar:
	./mvnw sonar:sonar

.PHONY: sonar-scanner
sonar-scanner:
	sonar-scanner

.PHONY: test
test:
	tests/all.sh

# make exec ARGS="<args>"
.PHONY: exec
exec:
	make run

# make run ARGS="<args>"
.PHONY: run
run:
	make gradle-run

.PHONY: gradle-run
gradle-run:
	./gradlew run -P ARGS="${ARGS}"

.PHONY: mvn-exec
mvn-exec:
	./mvnw exec:java -Dexec.args="${ARGS}"

# make sbt-run ARGS="192.168.99.100:9092 test"
.PHONY: sbt-run
sbt-run:
	sbt "run ${ARGS}"

.PHONY: findbugs
findbugs:
	./mvnw compile
	./mvnw findbugs:findbugs
	./mvnw findbugs:gui

.PHONY: versioneye
versioneye:
	make mvn-versioneye
	make gradle-versioneye
	make sbt-versioneye

.PHONY: mvn-versioneye
mvn-versioneye:
	./mvnw versioneye:update

.PHONY: gradle-versioneye
gradle-versioneye:
	./gradlew versionEyeUpdate

.PHONY: sbt-versioneye
sbt-versioneye:
	sbt versioneye:updateProject

.PHONY: scalastyle
scalastyle:
	sbt scalastyle

.PHONY: docker-run
docker-run:
	docker run -ti --rm harisekhon/nagios-plugin-kafka ${ARGS}

.PHONY: docker-mount
docker-mount:
	docker run -ti --rm -v $$PWD:/npk harisekhon/nagios-plugin-kafka bash -c "cd /npk; bash"

.PHONY: mount
	make docker-mount
