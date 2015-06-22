package no.nextgentel.oss.akkatools.serializing

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.{ConfigFactory, Config}
import no.nextgentel.oss.akkatools.persistence.{DurableMessageReceived, DurableMessage}
import org.scalatest.{Matchers, FunSuite}

class JacksonJsonSerializerTest extends FunSuite with Matchers {

  val objectMapper = new ObjectMapper()
  JacksonJsonSerializer.init(objectMapper)
  val serializer = new JacksonJsonSerializer()

  test("serializer") {
    val a = Animal("our cat", 12, Cat("black", true))
    val bytes = serializer.toBinary(a)
    val ar = serializer.fromBinary(bytes, classOf[Animal]).asInstanceOf[Animal]
    assert( a == ar)
  }

  test("Registering the serializer works") {
    val system = ActorSystem("JacksonJsonSerializerTest", ConfigFactory.load("akka-tools-json-serializing.conf"))

    val serialization = SerializationExtension.get(system)
    assert( classOf[JacksonJsonSerializer] ==  serialization.serializerFor(classOf[Animal]).getClass)

    // Make sure our special classes are also picked up by our serializer
    assert( classOf[JacksonJsonSerializer] ==  serialization.serializerFor(classOf[DurableMessage]).getClass)
    assert( classOf[JacksonJsonSerializer] ==  serialization.serializerFor(classOf[DurableMessageReceived]).getClass)

    system.shutdown()
  }

  test("DepricatedTypeWithMigrationInfo") {
    val bytes = serializer.toBinary(OldType("12"))
    assert(NewType(12) == serializer.fromBinary(bytes, classOf[OldType]))
  }



}

case class Animal(name:String, age:Int, t:Cat) extends JacksonJsonSerializable

case class Cat(color:String, tail:Boolean)

case class OldType(s:String) extends DepricatedTypeWithMigrationInfo {
  override def convertToMigratedType(): AnyRef = NewType(s.toInt)
}
case class NewType(i:Int)