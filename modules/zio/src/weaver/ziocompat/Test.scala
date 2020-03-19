package weaver.ziocompat

import weaver.Expectations
import weaver.Log
import weaver.Result
import weaver.TestOutcome

import cats.data.Chain
import zio._
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

class Test[R <: Has[_]](
    val name: String,
    val f: ZIO[PerTestEnv[R], Throwable, Expectations]) {

  def compile: ZIO[Env[R], Nothing, TestOutcome] =
    for {
      ref   <- Ref.make(Chain.empty[Log.Entry])
      start <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      res <- f
        .provideSomeLayer[Env[R]](ZLayer.succeed(RefLog(ref)))
        .fold(Result.from, Result.fromAssertion)
      end  <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)
}

object Test {

  def apply[R <: Has[_]](name: String)(
      f: ZIO[PerTestEnv[R], Throwable, Expectations]): Test[R] =
    new Test(name, f)

}
