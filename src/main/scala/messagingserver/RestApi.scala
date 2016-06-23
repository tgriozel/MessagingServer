package messagingserver

import akka.actor._

import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._

class RestApi extends HttpServiceActor with RestRoute {
  def receive = runRoute(restRoute)
}

trait RestRoute extends HttpService with Authenticator {
  import Protocol._

  val actorSystem = ActorSystem("messagingserver")
  implicit val executionContext = actorSystem.dispatcher

  val restRoute = {
    pathPrefix("createUser") {
      pathEnd {
        post {
          entity(as[User]) { user => requestContext =>
            createResponder(requestContext) ! UserService.acceptNewUser(user)
          }
        }
      }
    } ~
    authenticate(basicUserAuthenticator) { authUser =>
      pathPrefix("authenticated") {
        path("user") {
          get { requestContext =>
            createResponder(requestContext) ! authUser
          } ~
          delete { requestContext =>
            createResponder(requestContext) ! UserService.deleteUser(authUser.name)
          }
        } ~
        pathPrefix("properties") {
          pathEnd {
            post {
              entity(as[UserProperty]) { userProperty => requestContext =>
                createResponder(requestContext) ! UserService.suggestUserPropEdit(authUser, userProperty)
              }
            }
          } ~
          path(Segment) { propName =>
            get { requestContext =>
              val result = authUser.properties.get(propName) match {
                case Some(propValue: String) => UserProperty(propName, propValue)
                case _ => false
              }
              createResponder(requestContext) ! result
            } ~
            delete { requestContext =>
              val emptyProp = new UserProperty(propName, "")
              createResponder(requestContext) ! UserService.suggestUserPropEdit(authUser, emptyProp)
            }
          }
        } ~
        path("messages") {
          post {
            entity(as[Message]) { message => requestContext =>
              message.senderName = authUser.name
              createResponder(requestContext) ! MessagingService.addMessage(message)
            }
          } ~
          get { requestContext =>
            createResponder(requestContext) ! MessagingService.getMessage(authUser).getOrElse(false)
          }
        }
      }
    }
  }

  private def createResponder(requestContext:RequestContext) = {
    actorSystem.actorOf(Props(new Responder(requestContext)))
  }
}

class Responder(requestContext:RequestContext) extends Actor with ActorLogging {
  import Protocol._

  def receive = {
    case true =>
      requestContext.complete(StatusCodes.OK)
      terminate()

    case false =>
      requestContext.complete(StatusCodes.Conflict)
      terminate()

    case user: User =>
      requestContext.complete(StatusCodes.OK, user)
      terminate()

    case property: UserProperty =>
      requestContext.complete(StatusCodes.OK, property)
      terminate()

    case message: Message =>
      requestContext.complete(StatusCodes.OK, message)
      terminate()
  }

  private def terminate() = self ! PoisonPill
}
