FP GraphQL Scala
=================

# 1. Project

This project is used to train functional programming, as well as code organization and cleanliness and test coverage.

The main goal is to exploit functional programming techniques in Scala to develop a backend application exposing the
following GraphQL schema:

```
type News {
  title: String,
  link: String,
}

type Query {
  news: [News!]!
}
```

Then, a crawler is created to scrape all news headlines from nytimes.com and expose them using the GraphQL API. The news
are persisted to store all headlines collected using the following schema:

```
CREATE TABLE headlines (
  link VARCHAR PRIMARY KEY,
  title VARCHAR NOT NULL
);
```

# 2. Solution

## Description

The developed solution includes the following **Scala libraries**:

- Sangria
- Http4s
- Cats-Effect 3
- Zio
- Zio Interop Cats
- Scala-scrapper
- Quill
- Doobie
- Postgresql Driver
- log4cats-slf4j

And the **SBT plugins**:

- sbt-resolver
- sbt-scoverage
- sbt-dotenv
- sbt-native-packager

The code format is configured with ScalaFormat in the file `.scalafmt.conf`.

The project is organized into four subprojects:

- `core`: Project containing definition for the domain, repository and apis;
- `http-server`: Project responsible for building the Http Server using http4s to expose the GraphQL api;
- `web-scraper`: Project responsible for building the scraper application;
- `integration-tests`: Project responsible only for integration tests, combining the execution of the web scraper and
  the http server.

## Executing the project

The project can be executed in two different ways: `sbt` or `docker`.

### Database Schema

The database is automatically created when booting the Postgresql server. It executes on boot the script
`docker/docker-entrypoint-initdb.d/create-schema.sql`.

### Using sbt

> NOTE. Make sure you have sbt and docker properly configured, updated and running.

```bash
# start Postgres
docker compose -f docker/docker-compose.yml up

# build the http server and the web scraper
sbt clean stage

# start the web scraper in one terminal
web-scraper/target/universal/stage/bin/web-scraper-io-app

## with ZIO
web-scraper/target/universal/stage/bin/web-scraper-zio-app

# start the http server in another terminal
## Either server-io for Cats Effect
http-server/target/universal/stage/bin/server-io

## or z-server for ZIO Effect
http-server/target/universal/stage/bin/z-server
```

### Using docker compose

The file `docker/docker-compose-full.yml` contains the required manifest to execute all projects, i.e., the http server,
the web scraper and the Postgresql server. Note that the images have already been published
on [DockerHub](https://hub.docker.com/repositories):

- `igobrilhante/httpserver:0.2.0`
- `igobrilhante/webscraper:0.2.0`

Therefore, to run all the projects we can use the script below to start everything up.
> NOTE. Make sure you have docker properly configured, updated and running.

```bash
./run-with-docker.sh
```

You should see the three containers as follows.

```bash
[+] Running 4/4
 ⠿ Network docker_default           Created                                                                                                                   5.5s
 ⠿ Container docker_postgres_1      Created                                                                                                                   0.1s
 ⠿ Container docker_http-server_1   Created                                                                                                                   0.1s
 ⠿ Container docker_web-scrapper_1  Created                                                                                                                   0.1s
```

Doing some queries.

**Get a list of news**

```bash
curl --location --request POST 'localhost:8080/news' \
--header 'Content-Type: application/json' \
--data '{"query":"query GetNews { news {        title, link    }}","variables":{}}'
```

**Get news by link**

```bash
curl --location --request POST 'localhost:8080/news' \
--header 'Content-Type: application/json' \
--data '{"query":"query GetNewsByLink($link: String!) {\n    newsByLink(link: $link)  { title, link } }","variables":{"link":"https://www.nytimes.com/puzzles/spelling-bee"}}'
```

## Tests

The script `run-tests.sh` is a helper to execute the tests for this project.

```bash
./run-tests.sh
```

Once the tests finish, the aggregated coverage test report can be access as follows.

```bash
open target/scala-2.13/scoverage-report/index.html
```

The script `run-integration-tests.sh` is a helper to execute the integration tests configured in the
project `integration-tests`.

```bash
./run-integration-tests.sh
```

## Changelog

### Version 0.2.0

The main improvements done are listed below.

- Reimplemented the scrape application using functional libs: Cats, Cats Effect and ZIO. Two version are made available,
  one for each effect
- In the ZIO version, ZIO#retry was used to perform retry policy when some unavailability is found, like the website and
  the database
- Created test suites for IO and ZIO applications
- WebScraper definition was better organized to make tests easier
- Project integrationTests was organized to have tests in IntegrationTest scope of sbt
- Separate the tests script into: `run-tests` and `run-integration-tests`
- Added `log4cats-slf4j` in `web-scraper` project