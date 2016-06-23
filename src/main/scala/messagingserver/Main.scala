package messagingserver

import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem("messagingserver")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val config = com.typesafe.config.ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val api = system.actorOf(Props(classOf[RestApi]), "RestApi")

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface cannot bind to " + s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }
}
