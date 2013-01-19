#!/bin/sh
CLASSPATH="../jar/jnode-0.5.5.7.jar";
for I in ../lib/*.jar; do
    CLASSPATH="$CLASSPATH:$I"
done
java -cp "$CLASSPATH" jnode.main.Main ../config/jnode.conf