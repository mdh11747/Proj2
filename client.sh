#!/bin/bash

# Arguments 
ip_address=$1 nport=$2 tport=$3

javac Server/ClientHandler.java Server/myftpserver.java 
javac Client/myftp.java

java Client.myftp $ip_address $nport $tport
