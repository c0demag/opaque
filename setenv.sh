#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
export SPARK_LOCAL_IP="127.0.0.1"
export SPARKSGX_DATA_DIR=$DIR/data
export PRIVATE_KEY_PATH=$DIR/private_key.pem

