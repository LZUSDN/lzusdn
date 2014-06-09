#!/bin/bash
curl -d '{"switch": "00:00:00:00:00:00:00:01", "name":"s1flow1", "priority":"32768", "ingress-port":"2","active":"true", "actions":"output=3"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -d '{"switch": "00:00:00:00:00:00:00:01", "name":"s1flow2", "priority":"32768", "ingress-port":"3","active":"true", "actions":"output=2"}' http://localhost:8080/wm/staticflowentrypusher/json

curl -d '{"switch": "00:00:00:00:00:00:00:02", "name":"s2flow1", "priority":"32768", "ingress-port":"1","active":"true", "actions":"output=2"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -d '{"switch": "00:00:00:00:00:00:00:02", "name":"s2flow2", "priority":"32768", "ingress-port":"2","active":"true", "actions":"output=1"}' http://localhost:8080/wm/staticflowentrypusher/json

curl -d '{"switch": "00:00:00:00:00:00:00:04", "name":"s4flow1", "priority":"32768", "ingress-port":"1","active":"true", "actions":"output=3"}' http://localhost:8080/wm/staticflowentrypusher/json
curl -d '{"switch": "00:00:00:00:00:00:00:04", "name":"s4flow2", "priority":"32768", "ingress-port":"3","active":"true", "actions":"output=1"}' http://localhost:8080/wm/staticflowentrypusher/json

