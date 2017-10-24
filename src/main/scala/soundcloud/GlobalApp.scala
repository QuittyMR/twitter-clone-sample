package soundcloud

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import soundcloud.api.RegistrationHandler

import scala.collection.JavaConverters._
import scala.language.{implicitConversions, postfixOps}

/**
  * Entry point and global utility collection.
  * Should be imported in any class or object that requires global context (provided implicitly).
  */
object GlobalApp extends App with GlobalActorSystem with GlobalConfiguration {
	implicit val log: LoggingAdapter = Logging(system, getClass)
	system.actorOf(Props(new RegistrationHandler()))
}

/**
  * Context for the actor-system, including materializers and execution-contexts which aren't relevant without routers.
  * Custom actor configuration goes here, if you're running more cores than i am and want to optimize performance.
  */
trait GlobalActorSystem {
	implicit val system: ActorSystem = ActorSystem("global-actor")
}

/**
  * Configuration object.
  * Collects all environment variables prefixed with 'CONFIG', using double-underscore as a separator, and uses those
  * to override default config values -
  * implemented as a less bothersome replacement for Puppet/Ansible, and to support Kubernetes config-maps
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