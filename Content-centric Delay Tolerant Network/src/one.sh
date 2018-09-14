#! /bin/sh
java -Xms512M -Xmx3G -cp .:lib/ECLA.jar:lib/log4j-1.2.16.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
pause