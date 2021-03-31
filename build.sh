#/bin/bash

set -eux

mvn -B package

[ -d tmp ] && rm -r tmp

mkdir -p tmp/dist

curl -sSL -o tmp/commons-daemon.zip https://ftp.jaist.ac.jp/pub/apache/commons/daemon/binaries/windows/commons-daemon-1.2.4-bin-windows.zip
curl -sSL -o tmp/jdk.zip https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.10%2B9/OpenJDK11U-jdk_x86-32_windows_hotspot_11.0.10_9.zip

unzip -d tmp/commons-daemon tmp/commons-daemon.zip
unzip -d tmp/jdk tmp/jdk.zip

jlink --compress=2 --no-header-files --no-man-pages --output dist/jre --module-path tmp/dist/jdk/jdk-11.0.10+9/jmods --add-modules java.base,java.logging,java.management,java.naming,java.sql,java.xml

cp tmp/commons-daemon/prunmgr.exe tmp/dist/file-backup.exe
cp tmp/commons-daemon/amd64/prunsrv.exe tmp/dist/prunsrv.exe

cp app/target/file-backup-0.1.0-SNAPSHOT.jar tmp/dist/file-backup-0.1.0-SNAPSHOT.jar

cp scripts/install.bat tmp/dist/install.bat
cp scripts/delete.bat tmp/dist/delete.bat

cd tmp/dist
zip -r ../file-backup-0.1.0-SNAPSHOT.zip .
cd ../..
