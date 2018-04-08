package com.lightbend.akka.sample

import akka.actor.{Actor, ActorLogging, Props}


/**
  * Created by Adrián on 25/03/2018.
  */

object Device {
  def props(groupId: String, deviceId:String): Props = Props(new Device(groupId, deviceId))

  final case class RecordTemperature(requestId: Long, value: Double)
  final case class TemperatureRecorded(requestId: Long)

  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value: Option[Double])
}
class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info(s"Device actor {$groupId}-{$deviceId} started")
  override def postStop(): Unit = log.info(s"Device actor {$groupId}-{$deviceId} stopped")

  override def receive: Receive = {
    /*
    * We use `groupId` like a patter maching. it means, if we recive a RequestTrackDevice
    * that contains a groupId equals to our groupId (same for deviceId), the first case
    * will execute
    */
    case DeviceManager.RequestTrackDevice(`groupId`, `deviceId`) =>
      sender() ! DeviceManager.DeviceRegistered

    case DeviceManager.RequestTrackDevice(groupId, deviceId) =>
      log.warning(
        "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
        groupId, deviceId, this.groupId, this.deviceId
      )

    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)

    case RecordTemperature(id, value) =>
      log.info(s"Recorded temperature reading {$value} with {$id]")
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)
  }
}
