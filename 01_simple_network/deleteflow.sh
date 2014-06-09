#!/bin/bash

curl -X DELETE -d '{"name":"s1flow1"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -X DELETE -d '{"name":"s1flow2"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -X DELETE -d '{"name":"s2flow1"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -X DELETE -d '{"name":"s2flow2"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -X DELETE -d '{"name":"s4flow1"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -X DELETE -d '{"name":"s4flow2"}' http://localhost:8080/wm/staticflowentrypusher/json
