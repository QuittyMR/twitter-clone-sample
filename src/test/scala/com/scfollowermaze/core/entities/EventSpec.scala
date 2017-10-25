package com.scfollowermaze.core.entities

import org.scalatest.FlatSpec

class EventSpec extends FlatSpec {
	private val events: Map[Char, (String, Event)] = Map(
		'F' -> ("1|F|2|3", Event(1,'F',Option(2), Option(3))),
		'U' -> ("2|U|2|3", Event(2,'U',Option(2), Option(3))),
		'B' -> ("3|B", Event(3,'B')),
		'P' -> ("4|P|2|3", Event(4,'P',Option(2), Option(3))),
		'S' -> ("5|P|2", Event(5,'P',Option(2)))
	)

	"An event" should "be successfully instantiated from a string" in {
		events.valuesIterator.foreach(eventTuple =>
			assert(Event(eventTuple._1).equals(eventTuple._2))
		)
	}

	it should "be properly parsed to a string from an object" in {
		events.valuesIterator.foreach(eventTuple =>
			assert(eventTuple._2.toString.equals(eventTuple._1))
		)
	}

	it should "be sortable implicitly by the sequence id" in {
		val eventsList: List[Event] = events.values.map(_._2).toList.sorted
		(1 until eventsList.length).foreach(index =>
			assert(eventsList(index-1) < eventsList(index))
		)
	}
}