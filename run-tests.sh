#!/bin/bash


# execute the test coverage
sbt clean coverage test coverageReport coverageAggregate


