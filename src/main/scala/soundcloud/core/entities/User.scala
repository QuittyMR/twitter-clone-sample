package soundcloud.core.entities

import akka.actor.ActorRef

import scala.collection.mutable.ArrayBuffer

case class User(id: Int, actor: ActorRef, followers: ArrayBuffer[Int] = ArrayBuffer()) {
}

object UserRepository {

	private var users: Map[Int, User] = Map()

	def add(userId: Int, actor: ActorRef): Unit = users += userId -> User(userId, actor)

	def follow(userId: Int, followerId: Int): Option[User] = {
		get(userId) match {
			case Some(user) =>
				user.followers += followerId
				Option(user)
			case _ =>
				None
		}
	}

	def unfollow(userId: Int, followerId: Int): Unit = {
		get(userId) match {
			case Some(user) =>
				user.followers -= followerId
			case _ =>
				None
		}
	}

	def get(id: Int): Option[User] = users.get(id)

	def getByHandler(actorRef: ActorRef): Option[User] = users.values.find(_.actor == actorRef)

	def notify(user: User, message: String): Unit = {
		user.actor ! message
	}

}
