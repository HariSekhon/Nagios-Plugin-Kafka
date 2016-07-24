#  vim:ts=4:sts=4:sw=4:noet
#
#  Author: Hari Sekhon
#  Date: 2016-06-06 22:57:08 +0100 (Mon, 06 Jun 2016)
#
#  vim:ts=4:sts=4:sw=4:noet
#
#  https://github.com/harisekhon/nagios-plugin-kafka
#
#  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help improve or steer this or other code I publish
#
#  http://www.linkedin.com/in/harisekhon
#

ARGS=localhost:9092 test

.PHONY: build
build:
	make sbt

.PHONY: sbt
sbt:
	make lib-sbt
	sbt clean assembly
	cp -av target/scala-*/check_kafka-assembly-*.jar check_kafka.jar

.PHONY: mvn
mvn:
	make lib-mvn
	cd lib && mvn deploy:deploy-file -Durl=file://$$PWD/../repo -Dfile=$$(echo target/harisekhon-utils-*.jar) -DgroupId=com.linkedin.harisekhon -DartifactId=utils -Dpackaging=jar -Dversion=1.0
	mvn clean package
	cp -av target/check_kafka-*.jar check_kafka.jar

.PHONY: lib-mvn
lib-mvn:
	git submodule update --init
	cd lib && make mvn

.PHONY: lib
lib-sbt:
	git submodule update --init
	cd lib && make sbt
	sbt eclipse || echo "Ignore this last error, you simply don't have the SBT eclipse plugin, it's optional"

.PHONY: clean
clean:
	cd lib && make clean
	mvn clean || :
	sbt clean || :
	rm -f check_kafka.jar

.PHONY: update
update:
	git pull
	#git submodule update --init
	make

.PHONY: update2
update2:
	make update-no-recompile

.PHONY: update-no-recompile
update-no-recompile:
	git pull
	git submodule update --init --recursive

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

.PHONY: test
test:
	tests/all.sh

# make run ARGS="192.168.99.100:9092 test"
.PHONY: run
run:
	sbt "run ${ARGS}"

# make exec ARGS="<args>"
.PHONY: exec
exec:
	mvn exec:java -Dexec.args="${ARGS}"
