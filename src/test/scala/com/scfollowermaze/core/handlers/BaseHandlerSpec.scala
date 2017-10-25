package com.scfollowermaze.core.handlers

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.scfollowermaze.GlobalConfiguration
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class BaseHandlerSpec extends TestKit(ActorSystem("global-actor-test"))
	with DefaultTimeout with ImplicitSender
	with FlatSpecLike with Matchers with BeforeAndAfterAll
	with GlobalConfiguration {

	protected val socketAddress: Map[String, InetSocketAddress] = Map(
		"local" -> new InetSocketAddress("127.0.0.1", 4242),
		"client" -> new InetSocketAddress(config.getString("server.host"), config.getInt("server.client.port")),
		"event" -> new InetSocketAddress(config.getString("server.host"), config.getInt("server.event.port")),
		"remote" -> new InetSocketAddress(config.getString("server.host"), 4242)
	)

	override def afterAll {
		shutdown()
	}
}