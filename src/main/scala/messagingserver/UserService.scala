package messagingserver

import messagingserver.Protocol._

import scala.collection.{mutable => m}

object UserService {
  private val userMap = new m.HashMap[String, User]()

  def acceptNewUser(user: User): Boolean = this.synchronized {
    !userMap.contains(user.name) && userMap.put(user.name, user).isEmpty
  }

  def deleteUser(userName: String): Boolean = this.synchronized {
    userMap.contains(userName) && userMap.remove(userName).isDefined
  }

  def getUser(userName: String): Option[User] = this.synchronized {
    userMap.get(userName)
  }

  def suggestUserPropEdit(user: User, property: UserProperty): Boolean = this.synchronized {
    // Here we can imagine a strategy, with some properties being read-only or totally hidden
    // This will return false if the edit suggestion is refused, true otherwise
    // Deleting a property must be requested with the submission of an empty property value
    if (property.propValue.isEmpty)
      user.properties.remove(property.propName)
    else
      user.properties.put(property.propName, property.propValue)
    true
  }
}
