package com.lightbend.akka.sample

/**
  * Created by Adrián on 18/03/2018.
  */

import akka.actor.{Actor, ActorLogging, Props}

object IotSupervisor {
  def props():Props = Props(new IotSupervisor)
}

class IotSupervisor extends Actor with ActorLogging{
  override def preStart(): Unit = log.info("Iot Application started")
  override def postStop(): Unit = log.info("Iot Application stoped")

  override def receive: Receive = Actor.emptyBehavior
}
