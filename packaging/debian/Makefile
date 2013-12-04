TARGET_DIR=../../target
JAR_FILE=$(notdir $(wildcard $(TARGET_DIR)/stacksync-server-[0-9.]*-jar-with-dependencies.jar))

#PACKAGE_NAME=$(shell echo $(JAR_FILE) | sed -e 's/\(-jar-with-dependencies.jar\)//g')
PACKAGE_NAME=stacksync-server
VERSION=$(shell echo $(JAR_FILE) | sed -e 's:stacksync-server-\(.*\)-jar-with-dependencies.jar:\1:g')
CONFIG_DIR=../..
COMMONS_DIR=../common

all:
	@echo 'Please choose a target from the Makefile.'

.PHONY: clean

compile:
	cd ../..; mvn clean; mvn assembly:assembly

package: clean
	# Add changes to changelog
	#dch -v $(VERSION)
	# Compile source
	#cd ../../; mvn clean assembly:assembly
	# Create package folder and folder structure
	mkdir -p $(PACKAGE_NAME)/usr/lib/stacksync-server/lib
	mkdir -p $(PACKAGE_NAME)/usr/bin
	mkdir -p $(PACKAGE_NAME)/usr/share/man/man1
	mkdir -p $(PACKAGE_NAME)/var/log/stacksync-server
	mkdir -p $(PACKAGE_NAME)/etc/stacksync-server
	mkdir -p $(PACKAGE_NAME)/etc/init.d
	# Copy DEBIAN folder
	cp -r debian $(PACKAGE_NAME)/DEBIAN
	# Copy the JAR file
	cp $(TARGET_DIR)/$(JAR_FILE) $(PACKAGE_NAME)/usr/lib/stacksync-server/stacksync-server.jar
	# Copy bin script
	cp $(COMMONS_DIR)/stacksync-server $(PACKAGE_NAME)/usr/bin/
	# Copy commons-daemon lib
	cp $(COMMONS_DIR)/commons-daemon-1.0.15.jar $(PACKAGE_NAME)/usr/lib/stacksync-server/lib/
	# Copy man page
	cp $(COMMONS_DIR)/stacksync-server.1.gz $(PACKAGE_NAME)/usr/share/man/man1/
	# Copy configuration file
	cp $(CONFIG_DIR)/config.properties $(PACKAGE_NAME)/etc/stacksync-server/stacksync-server.conf
	# Move init script
	mv $(PACKAGE_NAME)/DEBIAN/stacksync-server.init $(PACKAGE_NAME)/etc/init.d/stacksync-server
	# FIXME: Calculate package size
	#INSTALLED_SIZE=$(du -sx --exclude DEBIAN $(PACKAGE_NAME) | cut -f1)
	# TODO: Update package size
	#sed -i 's/INSTALLED_SIZE/$(INSTALLED_SIZE)/g' $(PACKAGE_NAME)/DEBIAN/control
	# Update version
	sed -i 's/VERSION/$(VERSION)/g' $(PACKAGE_NAME)/DEBIAN/control
	# Generate md5sum file
	cd $(PACKAGE_NAME); find . -type f ! -regex '.*.hg.*' ! -regex '.*?debian-binary.*' ! -regex '.*?DEBIAN.*' -printf '%P ' | xargs md5sum > DEBIAN/md5sums
	# Create debian package
	dpkg --build $(PACKAGE_NAME) stacksync-server_$(VERSION)_all.deb
	

clean:
	rm -rf $(PACKAGE_NAME)
	


    
