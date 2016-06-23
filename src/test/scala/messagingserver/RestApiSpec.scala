package messagingserver

import spray.http.HttpHeaders.Authorization
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.testkit.Specs2RouteTest

import org.specs2.mutable.Specification

import messagingserver.Protocol._

class RestApiSpec extends Specification with Specs2RouteTest with RestRoute {
  def actorRefFactory = system

  val name1 = "thomas"
  val name2 = "vincent"
  val password = "password"
  val user1auth = Authorization(BasicHttpCredentials(name1, password))
  val user2auth = Authorization(BasicHttpCredentials(name2, password))
  val user1 = HttpEntity(ContentTypes.`application/json`,s"""{"name": "$name1", "password": "$password"}""")
  val user2 = HttpEntity(ContentTypes.`application/json`,s"""{"name": "$name2", "password": "$password"}""")
  val property = new UserProperty("email", "test@test.com")
  val message = Message(senderName = name1, recipientName = name2, topic = "test", body = "This is a test")

  // Those tests have to be launched sequentially, this is required
  sequential

  "The Rest service" should {

    "Create valid users" in {
      Post("/createUser", user1) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
      Post("/createUser", user2) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
    }

    "Refuse to create a user with an existing ID" in {
      Post("/createUser", user1) ~> restRoute ~> check {
        status mustEqual StatusCodes.Conflict
      }
    }

    "Refuse to access protected path with no authentication" in {
      Get("/authenticated/user") ~> sealRoute(restRoute) ~> check {
        status mustNotEqual StatusCodes.OK
      }
    }

    "Retrieve information about the users once authenticated" in {
      // User marshalling is not straightforward because of the password, so we use some hacks
      Get("/authenticated/user") ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        responseAs[String] must contain(name1)
      }
      Get("/authenticated/user") ~> addHeader(user2auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        responseAs[String] must contain(name2)
      }
    }

    "Set user properties with no problem" in {
      Post("/authenticated/properties", property) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
    }

    "Retrieve user properties correctly" in {
      Get("/authenticated/properties/" + property.propName) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        responseAs[UserProperty].propValue mustEqual property.propValue
      }
    }

    "Deleting a property works" in {
      Delete("/authenticated/properties/" + property.propName) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
      Get("/authenticated/properties/" + property.propName) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.Conflict
      }
    }

    "Send messages and receive then with no problem or content loss" in {
      val message2 = message.copy(topic = "anotherTopic", body = "anotherBody")
      Post("/authenticated/messages", message) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
      Post("/authenticated/messages", message2) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
      Get("/authenticated/messages") ~> addHeader(user2auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        val response = responseAs[Message]
        response.senderName mustEqual name1
        response.recipientName mustEqual name2
        response.topic mustEqual message.topic
        response.body mustEqual message.body
      }
      Get("/authenticated/messages") ~> addHeader(user2auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        val response = responseAs[Message]
        response.senderName mustEqual name1
        response.recipientName mustEqual name2
        response.topic mustEqual message2.topic
        response.body mustEqual message2.body
      }
    }

    "Consume messages as expected" in {
      Get("/authenticated/messages") ~> addHeader(user2auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.Conflict
      }
    }

    "Not be fooled by any fake sender field" in {
      val modMessage = message.copy(senderName = "fakeSender")
      Post("/authenticated/messages", modMessage) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
      }
      Get("/authenticated/messages") ~> addHeader(user2auth) ~> restRoute ~> check {
        status mustEqual StatusCodes.OK
        entity mustNotEqual None
        responseAs[Message].senderName mustEqual name1
      }
    }

    "Have an effective strategy to deal with lots of messages" in {
      for (i <- 1 to MessagingService.maxMessagesPerUser) {
        Post("/authenticated/messages", message) ~> addHeader(user1auth) ~> restRoute ~> check {
          status mustEqual StatusCodes.OK
        }
      }
      val expected = if (MessagingService.shouldDropOldMessages) StatusCodes.OK else StatusCodes.Conflict
      Post("/authenticated/messages", message) ~> addHeader(user1auth) ~> restRoute ~> check {
        status mustEqual expected
      }
    }
  }
}
