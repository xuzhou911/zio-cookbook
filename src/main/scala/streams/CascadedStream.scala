package streams
import Helper._

import zio.console.putStrLn
import zio.stream.ZStream
import zio.{ App, Chunk, Promise, Ref }

object App2 extends App {
  def run(args: List[String]) = app.exitCode

  val outerStream = ZStream.fromEffect(doWork(followers))

  val app = for {
    latch       <- Promise.make[Nothing, Unit]
    channelList <- Ref.make(List.empty[Chunk[Channel]])

    followerList <- outerStream // background stream
                     .drainFork( // run inner stream, which sets the latch when its done
                       (ZStream
                         .fromEffect(doWork(channels) <* latch.succeed(()))) // create stream from a zipped effect
                         .tap(channelList.set(_))                            // save inner stream output into ref
                     )
                     .runCollect // run, evaluate and collect outputs to the list

    _ <- putStrLn(channelList.toString)
    _ <- putStrLn(followerList.toString)

  } yield ()
}
