package com.lightbend.akka.sample

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.lightbend.akka.sample.DeviceManager.RequestTrackDevice

/**
  * Created by Adrián on 27/03/2018.
  */

object DeviceManager {
  def props(): Props = Props(new DeviceManager)

  final case class RequestTrackDevice(groupId: String, deviceId: String)
  case object DeviceRegistered
}
class DeviceManager extends Actor with ActorLogging{
  var groupIdToActorRef = Map.empty[String, ActorRef]
  var actorRefToGroupId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("Device Manager started")
  override def postStop(): Unit = log.info("Device Manager Stopped")

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(groupId, _) =>
      groupIdToActorRef.get(groupId) match {
        case Some(ref) =>
          ref forward trackMsg
        case None =>
          log.info(s"Creating device group actor  for ${groupId}")
          val groupActor = context.actorOf(DeviceGroup.props(groupId), "group-" + groupId)
          context.watch(groupActor)
          groupIdToActorRef += groupId -> groupActor
          actorRefToGroupId += groupActor -> groupId
          groupActor forward trackMsg
      }

    case Terminated(groupActor) =>
      val groupId = actorRefToGroupId(groupActor)
      log.info(s"Device group actor for ${groupId} has been terminated")
      groupIdToActorRef -= groupId
      actorRefToGroupId -= groupActor
  }
}
