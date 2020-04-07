#!/bin/bash

# 使子模块pom版本号与父项目保持一致
mvn -N versions:update-child-modules
