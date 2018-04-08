package com.lightbend.akka.sample

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by Adrián on 29/03/2018.
  */

object DeviceGroupQuery{
  case object CollectionTimeout

  def props(
           actorToDeviceId: Map[ActorRef, String],
           requestId: Long,
           requester: ActorRef,
           timeout: FiniteDuration
           ): Props = Props(new DeviceGroupQuery(actorToDeviceId,requestId,requester,timeout))
}

class DeviceGroupQuery(
                        actorToDeviceId: Map[ActorRef, String],
                        requestId: Long,
                        requester: ActorRef,
                        timeout: FiniteDuration
                      ) extends Actor with ActorLogging {
  import DeviceGroupQuery._
  import context.dispatcher

  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keySet.foreach{ deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(0)
    }
  }

  override def postStop(): Unit = queryTimeoutTimer.cancel()

  /*
  receive function doesn't handle the messages. This function returns a Receive function that
  handle the messages
   */
  override def receive: Receive = {
    waitingForReplies(Map.empty, actorToDeviceId.keySet)
  }

  def waitingForReplies(repliesSoFar: Map[String, DeviceGroup.TemperatureReading],
                        stillWaiting: Set[ActorRef]): Receive = {
    case Device.RespondTemperature(0, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => DeviceGroup.Temperature(value)
        case None => DeviceGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, DeviceGroup.DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies = stillWaiting.map{deviceActor =>
        val deviceId = actorToDeviceId(deviceActor)
        deviceId -> DeviceGroup.DeviceTimedOut
      }
      requester ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(deviceActor: ActorRef,
                       reading: DeviceGroup.TemperatureReading,
                       stillWaiting: Set[ActorRef],
                       repliesSoFar: Map[String, DeviceGroup.TemperatureReading]): Unit = {
    context.unwatch(deviceActor)
    val deviceID = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor
    val newRepliesSoFar = repliesSoFar + (deviceID -> reading)

    if(newStillWaiting.isEmpty){
      requester ! DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      /*
      become method change the actor's message handling function to the provided function
      When this method change the handling function to waitingForReplies(newRepliesSofar, newStillWaiting),
      the older repliesSoFar and newStillWaiting change too.

      Thanks to this method we can ensure the query consistency.
       */
      context.become(waitingForReplies(newRepliesSoFar,newStillWaiting))
    }
  }


}
