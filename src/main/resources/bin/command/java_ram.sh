#!/bin/bash

Name=$2
if [[ "$Name" = ""  ]]; then
   Name="java_ram.txt";
fi

echo ${Name}

touch ${Name}

: > ${Name}

jmap -heap $1 >> ${Name}

echo "===============================================================" >> ${Name}

jmap -histo $1 >> ${Name}

# jmap -dump:format=b,file=$Name -F $1

exit 0