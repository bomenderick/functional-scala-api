package com.sabalitech.api

import cats._
import cats.effect._
import com.sabalitech.BaseSpec
import com.sabalitech.db._
import com.sabalitech.models._
import com.sabalitech.models.TypeGenerators._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.server.Router

import scala.collection.immutable.Seq

/**
  * Created by Bomen Derick.
  */
final class ProductsRoutesTest extends BaseSpec {
  implicit def decodeProduct: EntityDecoder[IO, Product] = jsonOf
  implicit def decodeProducts: EntityDecoder[IO, List[Product]] = jsonOf
  implicit def encodeProduct[A[_] : Applicative]: EntityEncoder[A, Product] = jsonEncoderOf
  private val emptyRepository: Repository[IO] =  new TestRepository[IO](Seq.empty)

  "ProductsRoutes" when {
    "GET /products" when {
      "no products exist" must {
        val expectedStatusCode = Status.Ok

        s"return $expectedStatusCode and an empty list" in {
          def service: HttpRoutes[IO] =
            Router("/" -> new Routes(emptyRepository).routes)

          val response: IO[Response[IO]] = service.orNotFound.run(
            Request(method = Method.GET, uri = uri"/products")
          )
          val result = response.unsafeRunSync
          result.status must be(expectedStatusCode)
          result.as[List[Product]].unsafeRunSync mustEqual List.empty[Product]
        }
      }

      "products exist" must {
        val expectedStatusCode = Status.Ok

        s"return $expectedStatusCode and a list of products" in {
          forAll("products") { ps: List[Product] =>
            val repo: Repository[IO] = new TestRepository[IO](ps)

            def service: HttpRoutes[IO] =
              Router("/" -> new Routes(repo).routes)

            val response: IO[Response[IO]] = service.orNotFound.run(
              Request(method = Method.GET, uri = Uri.uri("/products"))
            )
            val result = response.unsafeRunSync
            result.status must be(expectedStatusCode)
            result.as[List[Product]].unsafeRunSync mustEqual ps
          }
        }
      }
    }

    "POST /products" when {
      "request body is invalid" must {
        val expectedStatusCode = Status.BadRequest

        s"return $expectedStatusCode" in {
          def service: HttpRoutes[IO] =
            Router("/" -> new Routes(emptyRepository).routes)

          val payload = scala.util.Random.alphanumeric.take(256).mkString
          val response: IO[Response[IO]] = service.orNotFound.run(
            Request(method = Method.POST, uri = Uri.uri("/products"))
              .withEntity(payload.asJson.noSpaces)
          )
          val result = response.unsafeRunSync
          result.status must be(expectedStatusCode)
          result.body.compile.toVector.unsafeRunSync must be(empty)
        }
      }

      "request body is valid" when {
        "product could be saved" must {
          val expectedStatusCode = Status.NoContent

          s"return $expectedStatusCode" in {
            forAll("product") { p: Product =>
              val repo: Repository[IO] = new TestRepository[IO](Seq(p))

              def service: HttpRoutes[IO] =
                Router("/" -> new Routes(repo).routes)

              val response: IO[Response[IO]] = service.orNotFound.run(
                Request(method = Method.POST, uri = Uri.uri("/products"))
                  .withEntity(p)
              )
              val result = response.unsafeRunSync
              result.status must be(expectedStatusCode)
              result.body.compile.toVector.unsafeRunSync must be(empty)
            }
          }
        }

        "product could not be saved" must {
          val expectedStatusCode = Status.InternalServerError

          s"return $expectedStatusCode" in {
            forAll("product") { p: Product =>
              def service: HttpRoutes[IO] =
                Router("/" -> new Routes(emptyRepository).routes)

              val response: IO[Response[IO]] = service.orNotFound.run(
                Request(method = Method.POST, uri = Uri.uri("/products"))
                  .withEntity(p)
              )
              val result = response.unsafeRunSync
              result.status must be(expectedStatusCode)
              result.body.compile.toVector.unsafeRunSync must be(empty)
            }
          }
        }
      }
    }
  }
}
