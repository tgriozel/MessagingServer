package messagingserver

import messagingserver.Protocol.User
import spray.routing.authentication.{UserPass, BasicAuth}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{Future, ExecutionContext}

trait Authenticator {
  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[User] = {
    def validateUser(userPass: Option[UserPass]): Option[User] = {
      for {
        p <- userPass
        user <- UserService.getUser(p.user)
        if user.passwordMatches(p.pass)
      } yield user
    }

    def authenticator(userPass: Option[UserPass]): Future[Option[User]] = Future(validateUser(userPass))

    BasicAuth(authenticator _, realm = "MessagingServer API")
  }
}
