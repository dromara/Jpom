#!/bin/bash

task_list=`launchctl list | grep docker | grep -v 'com.docker.helper' | awk '{print $3}'`
echo "docker task：${task_list}"
for task in ${task_list[@]}
do
    echo "${task}"
    echo "launchctl stop ${task}"
    launchctl stop ${task}
done
