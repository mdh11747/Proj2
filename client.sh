#!/bin/bash

# Arguments 
ip_address=$1 nport=$2 tport=$3

rm Client/myftp.class

javac Client/myftp.java

java Client.myftp $ip_address $nport $tport
