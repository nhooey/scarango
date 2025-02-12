package com.outr.arango.core

import profig.Profig

import scala.concurrent.duration._

case class ArangoDBConfig(username: String = Profig("arangodb.username").opt[String].orNull,
                          password: String = Profig("arangodb.password").opt[String].orNull,
                          ssl: Boolean = Profig("arangodb.ssl").as[Boolean](false),
                          timeout: FiniteDuration = Profig("arangodb.timeout").opt[Long].map(_.millis).orNull,
                          acquireHostList: Boolean = Profig("arangodb.acquireHostList").as[Boolean](false),
                          chunkSize: Int = Profig("arangodb.chunkSize").as[Int](-1),
                          connectionTtl: FiniteDuration = Profig("arangodb.connectionTtl").opt[Long].map(_.millis).orNull,
                          hosts: List[Host] = Profig("arangodb.hosts").as[List[Host]](List(Host())),
                          keepAliveInterval: FiniteDuration = Profig("arangodb.keepAliveInterval").opt[Long].map(_.millis).orNull,
                          loadBalancingStrategy: LoadBalancingStrategy = Profig("arangodb.loadBalancingStrategy").opt[LoadBalancingStrategy].getOrElse(LoadBalancingStrategy.None),
                          maxConnections: Int = Profig("arangodb.maxConnections").as[Int](1))