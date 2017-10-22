package soundcloud

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import soundcloud.api.registrationServer

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.{implicitConversions, postfixOps}

object globalApp extends App with globalActorSystem with globalConfiguration {
	implicit val log: LoggingAdapter = Logging(system, getClass)
	val server = system.actorOf(Props(new registrationServer()))
}

trait globalActorSystem {
	implicit val system: ActorSystem = ActorSystem("global-actor")
	implicit val materializer: ActorMaterializer = ActorMaterializer()
	implicit val executionContext: ExecutionContextExecutor = system.dispatcher
}

trait globalConfiguration {
	implicit var config: Config = ConfigFactory.load()

	System.getenv().asScala.filterKeys(
		_.startsWith("CONFIG__")
	).foreach(envVariable => {
		val confMap = envVariable._1.replace("CONFIG__", "").split("__").mkString(".") -> envVariable._2
		printf(s"${confMap._1} => ${confMap._2}\n")
		config = config.withValue(confMap._1, ConfigValueFactory.fromAnyRef(confMap._2))
	})

	implicit var timeout: Timeout = Timeout(500 seconds)
}