#!/bin/bash
java -Dspring.profiles.active=client -Dapplication.client.mode=cli -jar target/indexsearch-client-0.0.1-SNAPSHOT.jar
