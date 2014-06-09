#!/bin/bash

curl -X POST  -d '{"mac":"b8:ca:3a:73:1d:bb","net":"net1"}' http://202.201.3.39:8080/wm/networkisolation/network/json/
curl -X POST  -d '{"mac":"b8:ca:3a:73:1d:07","net":"net2"}' http://202.201.3.39:8080/wm/networkisolation/network/json/