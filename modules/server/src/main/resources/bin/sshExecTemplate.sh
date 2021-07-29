#!/bin/bash

# loading env
if [ -f /etc/profile ]; then
  . /etc/profile
fi
if [ -f /etc/bashrc ]; then
  . /etc/bashrc
fi
if [ -f ~/.bashrc ]; then
  . ~/.bashrc
fi
if [ -f ~/.bash_profile ]; then
  . ~/.bash_profile
fi

# start
