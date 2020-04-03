package com.outr.arango

import com.outr.arango.api.{APIWalTail, WALOperation, WALOperations}
import io.youi.client.HttpClient
import io.youi.util.Time
import reactify.Channel

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ArangoWriteAheadLog(client: HttpClient) {
  def tail(global: Boolean = false,
           from: Option[Long] = None,
           to: Option[Long] = None,
           lastScanned: Long = 0L,
           chunkSize: Option[Long] = None,
           syncerId: Option[Long] = None,
           serverId: Option[Long] = None,
           clientId: String = "scarango")
          (implicit ec: ExecutionContext): Future[WALOperations] = APIWalTail.get(
    client = client,
    global = Some(global),
    from = from,
    to = to,
    lastScanned = lastScanned,
    chunkSize = chunkSize,
    syncerId = syncerId,
    serverId = serverId,
    clientId = Some(clientId)
  )

  def monitor(global: Boolean = false,
              from: Option[Long] = None,
              to: Option[Long] = None,
              lastScanned: Long = 0L,
              chunkSize: Option[Long] = None,
              syncerId: Option[Long] = None,
              serverId: Option[Long] = None,
              clientId: String = "scarango",
              delay: FiniteDuration = 5.seconds,
              failureHandler: Throwable => Option[FiniteDuration] = t => {
                scribe.error("Monitor error", t)
                None
              })
             (implicit ec: ExecutionContext): WriteAheadLogMonitor = {
    val m = new WriteAheadLogMonitor(delay, failureHandler)
    m.run(tail(global, from, to, lastScanned, chunkSize, syncerId, serverId, clientId))
    m
  }
}

class WriteAheadLogMonitor(delay: FiniteDuration, failureHandler: Throwable => Option[FiniteDuration]) extends Channel[WALOperation] {
  private var keepAlive = false
  private var last: Option[WALOperations] = None

  private[arango] def run(future: Future[WALOperations])(implicit ec: ExecutionContext): Unit = {
    keepAlive = true

    future.onComplete { complete =>
      val d = complete match {
        case Success(operations) => try {
          operations.operations.foreach(static)
          last = Some(operations)
          Some(delay)
        } catch {
          case t: Throwable => failureHandler(t)
        }
        case Failure(exception) => failureHandler(exception)
      }
      d match {
        case Some(delay) if keepAlive => last.foreach { ops =>
          Time.delay(delay).foreach(_ => run(ops.tail()))
        }
        case _ => // Error or keepAlive caused monitor to stop
      }
    }
  }

  def stop(): Unit = keepAlive = false
}