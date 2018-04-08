package com.lightbend.akka.sample

/**
  * Created by Adrián on 18/03/2018.
  */

import akka.actor.ActorSystem
import scala.io.StdIn

object IotApp extends App{
  val system = ActorSystem("iot-system")

  try{
    //create top level supervisor
    val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")
    //EXIT THE SYSTEM WHEN ENTER IS PRESSED
    StdIn.readLine()
  } finally {
    system.terminate()
  }

}
