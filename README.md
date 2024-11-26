[![Gitter](https://badges.gitter.im/7mind/izumi.svg)](https://gitter.im/7mind/izumi)
[![Patreon](https://img.shields.io/badge/patreon-sponsor-ff69b4.svg)](https://www.patreon.com/7mind)
[![Build Status](https://dev.azure.com/7mind/izumi/_apis/build/status/7mind.izumi?branchName=develop)](https://dev.azure.com/7mind/izumi/_build/latest?definitionId=6&branchName=develop)
[![License](https://img.shields.io/github/license/7mind/idealingua-v1.svg)](https://github.com/7mind/idealingua-v1/blob/develop/LICENSE)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/lauris/awesome-scala)

<p align="center">
  <a href="https://izumi.7mind.io/">
  <img width="40%" src="https://github.com/7mind/izumi/blob/develop/doc/microsite/src/main/tut/media/izumi-logo-full-purple.png?raw=true" alt="Izumi"/>
  </a>
</p>

---

<p align="center">
  <a href="https://www.buymeacoffee.com/7mind"><img src="https://bmc-cdn.nyc3.digitaloceanspaces.com/BMC-button-images/custom_images/orange_img.png" alt="Izumi"/></a>
</p>

---

[![Latest Release](https://img.shields.io/github/tag/7mind/idealingua-v1.svg)](https://github.com/7mind/idealingua-v1/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.7mind.izumi/idealingua-v1-transpilers_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.7mind.izumi%22)
[![Sonatype releases](https://img.shields.io/nexus/r/https/oss.sonatype.org/io.7mind.izumi/idealingua-v1-transpilers_2.12.svg)](https://oss.sonatype.org/content/repositories/releases/io/7mind/izumi/)
[![Sonatype snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.7mind.izumi/idealingua-v1-transpilers_2.12.svg)](https://oss.sonatype.org/content/repositories/snapshots/io/7mind/izumi/)
[![Latest version](https://index.scala-lang.org/7mind/idealingua-v1/latest.svg?color=orange)](https://index.scala-lang.org/7mind/idealingua-v1)

## IdeaLingua RPC/DML

[*IdeaLingua*](https://izumi.7mind.io/latest/release/doc/idealingua/index.html) is an RPC framework & Domain Modeling Language, it’s purpose is to:

* Share & publish APIs and data models in a common concise format
* Allow remote calls to services given their public API definitions.
* Create idiomatic API clients and servers for all programming languages – currently Scala, TypeScript, C# & Go.
* Support frontend-to-backend and backend-to-frontend calls (ala push notifications, via buzzer definitions)
* Abstract away details such as the network protocol or the serialization format.
* Save developers from untyped and brittle REST.

*IdeaLingua* is a part of the [Izumi Project](https://github.com/7mind/izumi). Izumi (*jp. 泉水, spring*) is a set of independent libraries and frameworks allowing you to significantly increase productivity of your Scala development.

including the following components:

1. [distage](https://izumi.7mind.io/latest/release/doc/distage/) – Transparent and debuggable Dependency Injection framework for Pure FP Scala,
2. [logstage](https://izumi.7mind.io/latest/release/doc/logstage/) – Automatic structural logs from Scala string interpolations,
3. [idealingua](https://izumi.7mind.io/latest/release/doc/idealingua/) (moved to [7mind/idealingua-v1](https://github.com/7mind/idealingua-v1)) – API Definition, Data Modeling and RPC Language, optimized for fast prototyping – like gRPC, but with a human face. Currently generates servers and clients for Go, TypeScript, C# and Scala,
4. [Opinionated SBT plugins](https://izumi.7mind.io/latest/release/doc/sbt/) (moved to [7mind/sbtgen](https://github.com/7mind/sbtgen)) – Reduces verbosity of SBT builds and introduces new features – inter-project shared test scopes and BOM plugins (from Maven)
5. [Percept-Plan-Execute-Repeat (PPER)](https://izumi.7mind.io/latest/release/doc/pper/) – a pattern that enables modeling very complex domains and orchestrate deadly complex processes a lot easier than you're used to.

[VSCode Extension](https://marketplace.visualstudio.com/items?itemName=SeptimalMind.idealingua1) [VSCodium Extension](https://open-vsx.org/extension/SeptimalMind/idealingua1)

Docs
----

* **[Documentation](https://izumi.7mind.io/latest/release/doc/idealingua/index.html)**

Example projects:

* [Idealingua Example Project with TypeScript and Scala](https://github.com/7mind/idealingua-example)

Support Chats:

* [Izumi on Gitter](https://gitter.im/7mind/izumi)
* [Izumi User Group [EN] on Telegram](https://t.me/izumi_en)
* [Izumi User Group [RU] on Telegram](https://t.me/izumi_ru)

Key goals
=========

We aim to provide tools that:

1. Boost productivity and reduce code bloat
2. Are as non-invasive as possible
3. Are introspectable
4. Are better than anything else out there :3

Current state and future plans
==============================

We are looking for early adopters, contributors and sponsors.

This project is currently a work in progress.

In the future we are going to (or may) implement more tools based on PPER approach:

1. Best in the world build system
2. Best in the world cluster orchestration tool
3. Best in the world load testing/macro-benchmark tool

Credits
=======

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and
[YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

[![Triplequote Hydra](https://triplequote.com/img/services/hydra-2.svg)](https://triplequote.com/)

[Triplequote Hydra](https://triplequote.com/) is the world’s only parallel compiler for the Scala language. Hydra works by parallelizing all of the Scala compiler phases, taking full advantage of the many cores available in modern hardware.

Contributors
============

* Run `./sbtgen.sc` to generate a JVM-only sbt project, run `./sbtgen.sc --js` to generate a JVM+JS sbt crossproject

See:

- [Build notes](doc/md/build.md)
- [Project flow](doc/md/flow.md)
