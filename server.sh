#!/bin/bash

# Arguments 
nport=$1 tport=$2

rm Server/ClientHandler.class Server/myftpserver.class
javac Server/ClientHandler.java Server/myftpserver.java 

java Server.myftpserver $nport $tport
