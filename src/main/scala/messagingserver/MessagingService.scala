package messagingserver

import messagingserver.Protocol._

import scala.collection.{mutable => m}

object MessagingService {
  val maxMessagesPerUser = 100
  val shouldDropOldMessages = false
  private val messageCount= new m.HashMap[String, Int]()
  private val userMessages = new m.HashMap[String, m.Queue[Message]]()

  def addMessage(message: Message): Boolean = this.synchronized {
    val key = message.recipientName
    val queue = userMessages.get(key) match {
      case Some(userQueue: m.Queue[Message]) =>
        userQueue
      case None =>
        val newQueue = new m.Queue[Message]()
        userMessages.put(key, newQueue)
        newQueue
    }
    var currentCount = messageCount.getOrElse(key, 0)

    if (currentCount == maxMessagesPerUser) {
      if (!shouldDropOldMessages)
        return false

      queue.dequeue()
      currentCount = currentCount - 1
    }

    queue.enqueue(message)
    messageCount.put(key, currentCount + 1)
    true
  }

  def getMessage(user: User): Option[Message] = this.synchronized {
    val key = user.name
    val count = messageCount.getOrElse(key, 0)
    if (count == 0)
      return None
    userMessages.get(key) match {
      case None =>
        None
      case Some(userQueue: m.Queue[Message]) =>
        messageCount.put(key, count - 1)
        Option(userQueue.dequeue())
    }
  }
}
