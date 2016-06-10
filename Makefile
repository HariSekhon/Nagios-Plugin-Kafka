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

.PHONY: make
build:
	#make lib
	sbt clean assembly
	cp -av target/scala-*/check_kafka-assembly-*.jar check_kafka.jar

.PHONY: lib
lib:
	git submodule update --init
	cd lib && mvn clean package
	sbt eclipse || :

.PHONY: clean
clean:
	cd lib && mvn clean
	sbt clean
	rm -f check_kafka.jar

.PHONY: update
update:
	git pull
	git submodule update --init
	make

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
	git submodule update --init
	cd lib && mvn clean package
	sbt package

.PHONY: test
test:
	tests/all.sh
