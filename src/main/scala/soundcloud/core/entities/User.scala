package soundcloud.core.entities

import akka.actor.ActorRef
import soundcloud.core.Event

import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable
import scala.collection.convert.decorateAsScala._

case class User(id: Int, actor: Option[ActorRef], followers: ArrayBuffer[Int] = ArrayBuffer()) {
}

object UserRepository {

	import soundcloud.globalApp._

	private val users: mutable.Map[Int, User] = new ConcurrentHashMap[Int, User]().asScala

	def add(userId: Int, actor: Option[ActorRef]): User = {
		val user = User(userId, actor)
		users += (userId -> user)
		user
	}

	def follow(userId: Int, followerId: Int): User = {
		get(userId) match {
			case Some(user) =>
				user.followers += followerId
				user
			case _ =>
				log.debug(s"Falsifying record for $userId")
				add(userId, None)
				follow(userId, followerId)
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

	def get(userId: Int): Option[User] = users.get(userId)

	def getByHandler(actorRef: ActorRef): Option[User] = users.values.find(_.actor == actorRef)

	def getAll: Iterable[User] = users.values

	def notify(user: User, message: Event): Unit = {
		user.actor match {
			case Some(actor) =>
				actor ! (message.toString + "\n")
			case _ =>
				log.debug(s"User ${user.id} not registered")
		}
	}
}
