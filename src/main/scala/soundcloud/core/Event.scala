package soundcloud.core

case class Event(sequenceId: Int,
				 messageType: Char,
				 fromUser: Option[Int] = None,
				 toUser: Option[Int] = None) extends Ordered[Event] {

	override def compare(that: Event): Int = this.sequenceId.compareTo(that.sequenceId)

	override def toString = this.productIterator.collect {
			case Some(parameter) => parameter
			case parameter if parameter != None => parameter
		}.mkString("|")
}

object Event {
	def apply(data: String): Event = {
		val dataArray = data.split('|')

		Event(sequenceId = dataArray(0).toInt,
			messageType = dataArray(1).head,
			fromUser = dataArray.lift(2).map(_.toInt),
			toUser = dataArray.lift(3).map(_.toInt))
	}
}

