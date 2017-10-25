package com.scfollowermaze

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import com.scfollowermaze.api.RegistrationHandler
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.language.{implicitConversions, postfixOps}

/**
  * Entry point and global utility collection.
  * Should be imported in any class or object that requires global context (provided implicitly).
  */
object GlobalApp extends App with GlobalActorSystem with GlobalConfiguration {
	implicit val log: LoggingAdapter = Logging(system, getClass)
	system.actorOf(Props(new RegistrationHandler()), "registrationHandler")
}

/**
  * Context for the actor-system, including an execution context and default timeout fallback for tests.
  * Custom configuration goes here, if you're running more cores than i am and want to optimize performance. See config.
  */
trait GlobalActorSystem {
	implicit val system: ActorSystem = ActorSystem("sc-follower-maze")
	implicit val executionContext: ExecutionContextExecutor = system.dispatcher
	implicit val defaultTimeout: Timeout = Timeout(1,TimeUnit.SECONDS)
}

/**
  * Configuration object.
  * Collects all environment variables prefixed with 'CONFIG', using double-underscore as a separator, and uses those
  * to override default config values -
  * implemented as a less annoying replacement for Puppet/Ansible, and to support Kubernetes config-maps
  * for different environments.
  */
trait GlobalConfiguration {
	implicit var config: Config = ConfigFactory.load()

	System.getenv().asScala.filterKeys(
		_.startsWith("CONFIG__")
	).foreach(envVariable => {
		val confMap = envVariable._1.replace("CONFIG__", "").split("__").mkString(".") -> envVariable._2
		printf(s"${confMap._1} => ${confMap._2}\n")
		config = config.withValue(confMap._1, ConfigValueFactory.fromAnyRef(confMap._2))
	})
}
