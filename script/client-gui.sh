#!/bin/bash
java -Dspring.profiles.active=client -Dapplication.client.mode=gui -jar target/indexsearch-client-0.0.1-SNAPSHOT.jar
