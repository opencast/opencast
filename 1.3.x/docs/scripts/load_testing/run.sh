#!/bin/bash
rm loadtesting/*.class
javac loadtesting/*.java
java loadtesting.LoadTesting
