package soundcloud.core.entities

import akka.actor.ActorRef

import scala.collection.mutable.ArrayBuffer

case class User(id: Int, actor: ActorRef, followers: ArrayBuffer[Int] = ArrayBuffer()) {

}

object UserRepository {
	private var users: Map[Int, User] = Map()

	def add(userId: Int, actor:ActorRef): Unit = users += userId -> User(userId,actor,ArrayBuffer())

	def get(id: Int): Option[User] = users.get(id)

	def getByHandler(actorRef: ActorRef): Option[User] = users.values.find(_.actor == actorRef)

}
