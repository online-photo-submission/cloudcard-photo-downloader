#!/bin/sh 
openssl enc -d -aes-256-cbc -a -in run-sh-enc | sh - 