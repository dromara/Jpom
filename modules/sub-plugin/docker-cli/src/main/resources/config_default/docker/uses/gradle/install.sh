if [ $GRADLE_VERSION ]; then
	echo "GRADLE_VERSION ${GRADLE_VERSION}"
else
	echo "not found GRADLE_VERSION"
	exit 1
fi
cd /tmp
download_url="https://downloads.gradle-dn.com/distributions/gradle-${GRADLE_VERSION}-bin.zip"
apt install unzip
wget ${download_url} -O gradle-bin.zip
unzip -q -d /opt/gradle/ -o gradle-bin.zip
mv -f /opt/gradle/gradle-${GRADLE_VERSION}/* /opt/gradle
rm -rf /opt/gradle/gradle-${GRADLE_VERSION}
