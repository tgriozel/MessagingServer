package messagingserver

import org.mindrot.jbcrypt.BCrypt
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.{mutable => m}

object Protocol {
  // Model
  class User(val name: String, password: String) {
    val properties: m.Map[String, String] = new m.HashMap[String, String]()
    val permissions: m.Set[String] = new m.HashSet[String]()
    val salt: String = BCrypt.gensalt()
    private val saltedPassword: String = BCrypt.hashpw(password, salt)

    def setProperty(propKey: String, propValue: String): Unit = properties.put(propKey, propValue)
    def unSetProperty(propKey: String): Unit = properties.remove(propKey)
    def addPermission(permission: String): Unit = permissions.add(permission)
    def removePermission(permission: String): Unit = permissions.remove(permission)
    def passwordMatches(password: String): Boolean = saltedPassword == BCrypt.hashpw(password, salt)
  }

  case class UserProperty(propName: String, propValue: String)

  case class Message(var senderName: String = "", recipientName: String, topic: String, body: String)

  // JSON marshallers
  object User {

    implicit object UserFormat extends RootJsonFormat[User] {
      override def write(user: User): JsValue = {
        JsObject(
          ("name", JsString(user.name)),
          ("permissions", JsArray(user.permissions.map(_.toJson).toVector)),
          ("properties", JsArray(user.properties.map(_.toJson).toVector))
        )
      }

      override def read(value: JsValue): User = {
        value.asJsObject.getFields("name", "password") match {
          case Seq(JsString(name), JsString(password)) => new User(name, password)
          case _ => throw DeserializationException("User is not well-formed")
        }
      }
    }
  }

  object UserProperty {
    implicit val format = jsonFormat2(UserProperty.apply)
  }

  object Message {
    implicit val format = jsonFormat4(Message.apply)
  }
}
