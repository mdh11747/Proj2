#!/bin/bash

# Arguments 
nport=$1 tport=$2

javac Server/ClientHandler.java Server/myftpserver.java 
javac Client/myftp.java

java Server.myftpserver $nport $tport

