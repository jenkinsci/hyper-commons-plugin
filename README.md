hyper-commons-plugin
=====================

[![Build Status](https://travis-ci.org/jenkinsci/hyper-commons-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/hyper-commons-plugin)

This plugin provides common functionality for integrating Jenkins with Hyper_.

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Features](#features)
- [Use plugin](#use-plugin)
	- [Prerequisites](#prerequisites)
	- [Config plugin](#config-plugin)
	- [Config job](#config-job)
	- [Build job](#build-job)
	- [View build result](#view-build-result)
- [Build plugin](#build-plugin)
	- [Prerequisites](#prerequisites)
	- [Compile](#compile)
	- [Test](#test)
	- [Package](#package)
	- [Install hyper-commons-plugin](#install-hyper-commons-plugin)
		- [In local dev jenkins server](#in-local-dev-jenkins-server)
		- [In standalone jenkins server](#in-standalone-jenkins-server)
- [Release plugin](#release-plugin)

<!-- /TOC -->

# Features
Plugin currently supports the following features:

- Install hyper cli
- Set Hyper_ credentials
- Test connection to your account.


# Use plugin

## Prerequisites

- Jenkins
- hyper-commons
- Hyper_ credential(get from https://console.hyper.sh)

## Config plugin

```
Manage Jenkins -> Configure System -> Hyper Config
  -> Hyper_ Access Key
  -> Hyper_ Secret Key
  -> DockerHub Username
  -> DockerHub Password
  -> DockerHub Email

Manage Jenkins -> Configure System -> Hypercli Install
```
![](images/config-system.PNG)

## Config job
![](images/config-job.PNG)

## Build job
![](images/build-now.PNG)

## View build result
![](images/console-output.PNG)

# Build plugin

## Prerequisites

- java 1.8+
- maven 3+

## Compile
```
$ mvn compile
```

## Test

compile + test

```
$ mvn test
```

## Package

> **output**: target/hyper-commons.hpi

compile + test + package

```
$ mvn package

//skip test
$ mvn package -DskipTests
```

## Install hyper-commons-plugin

### In local dev jenkins server
```
//start jenkins server
$ mvn hpi:run -Djetty.port=8090

//open jenkins web ui
http://<host_ip>:8090/jenkins
```

### In standalone jenkins server

open Jenkins Web UI in web browser

get pre-build `hyper-commons.hpi`

```
Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin
```
![](images/upload-plugin.PNG)

# Release plugin
```
$ mvn release:prepare release:perform
```
