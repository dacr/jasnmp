package fr.janalyse.snmp

import java.io.IOException
import org.snmp4j._
import org.snmp4j.event._
import org.snmp4j.smi._
import org.snmp4j.util._
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.transport.DefaultTcpTransportMapping
import org.snmp4j.transport.DefaultUdpTransportMapping
import SnmpConstants.{ version1, version2c, version3 }
import collection.JavaConversions._

import akka.actor._
import ActorDSL._

class SNMPAsync(
  community: String,
  host: String = "127.0.0.1",
  port: Int = 161,
  timeout: Int = 3000,
  retry: Int = 2,
  version: Int = SNMP.v2c,
  protocol: String = "UDP") {

  val config = com.typesafe.config.ConfigFactory.load()
  implicit val system = ActorSystem(s"SnmpActorSystemFor_${host.replaceAll("[.]", "-")}_$port", config.getConfig("snmpasync").withFallback(config))

  private val target = {
    val target = new CommunityTarget()
    target.setCommunity(new OctetString(community))
    val address = GenericAddress.parse(protocol + ":" + host + "/" + port)
    target.setAddress(address)
    target.setVersion(version)
    target.setTimeout(timeout)
    target
  }

  private def transport(protocol: String) = {
    if (protocol.equalsIgnoreCase("UDP")) {
      new DefaultUdpTransportMapping()
    } else {
      new DefaultTcpTransportMapping()
    }
  }

  object SnmpActor {
    def props() = Props(new SnmpActor)
  }

  def syntax2Desc(s:Int):String = {
    import SMIConstants._
    s match {
      case EXCEPTION_END_OF_MIB_VIEW => "END_OF_MIB_VIEW"
      case EXCEPTION_NO_SUCH_INSTANCE  => "NO_SUCH_INSTANCE"
      case EXCEPTION_NO_SUCH_OBJECT  => "NO_SUCH_OBJECT"
      //case SYNTAX_BITS  => "BITS"
      case SYNTAX_COUNTER32  => "COUNTER32"
      case SYNTAX_COUNTER64  => "COUNTER64"
      case SYNTAX_GAUGE32  => "GAUGE32"
      case SYNTAX_INTEGER  => "INTEGER"
      //case SYNTAX_INTEGER32  => "INTEGER32"
      case SYNTAX_IPADDRESS  => "IPADDRESS"
      case SYNTAX_NULL  => "NULL"
      case SYNTAX_OBJECT_IDENTIFIER  => "OBJECT_IDENTIFIER"
      case SYNTAX_OCTET_STRING  => "OCTET_STRING"
      case SYNTAX_OPAQUE  => "OPAQUE"
      case SYNTAX_TIMETICKS  => "TIMETICKS"
      //case SYNTAX_UNSIGNED_INTEGER32  => "UNSIGNED_INTEGER32"
      case _ => "UNKNOWN"
    }
  }
  
  def dumpVariable(v:VariableBinding) {
    val oid = v.getOid.format()
    val value = v.getVariable
    val vtype = syntax2Desc(v.getSyntax)
    System.out.println(s" $oid = $value ($vtype)")
  }

  class SnmpActor extends Actor {
    def receive = {
      case event: ResponseEvent =>
        println("-----------------------")
        for { v <- event.getResponse.getVariableBindings } dumpVariable(v) 
      case event: TreeEvent =>
        println("-----------------------")
        for { v <- event.getVariableBindings } dumpVariable(v)
    }
  }

  private val snmpActor = actor("SnmpActor") {
    new SnmpActor()
  }

  class MyListener(receiver:ActorRef) extends ResponseListener {
    def onResponse(event: ResponseEvent) {
      event.getSource match {
        case snmp: Snmp =>
          snmp.cancel(event.getRequest, this)
          receiver ! event
      }
    }    
  }

  class MyTreeListener(receiver:ActorRef) extends TreeListener {
    var finishState=false
    def finished(event: TreeEvent) {
      finishState=true
    }
    def isFinished(): Boolean = finishState
    def next(event: TreeEvent): Boolean = {
      receiver ! event
      true
    }
  }

  val snmp = new Snmp(transport(protocol))
  val pduFactory = new DefaultPDUFactory()
  val utils = new TreeUtils(snmp, pduFactory)

  snmp.listen()

  // -----------------------------------------------------------------
  
  def walk(soid: String*) {
    walk(soid.map(new OID(_)).toArray)
  }

  def walk(oids: Array[OID]) {
    utils.walk(target, oids, null, new MyTreeListener(snmpActor))
  }

  def get(soid: String*) {
    get(soid.map(new OID(_)).toArray)    
  }
  
  def get(oids: Array[OID]) {
    val pdu = new PDU()
    pdu.addAll(oids.map(new VariableBinding(_)))
    snmp.get(pdu, target, null, new MyListener(snmpActor))
  }

  // -----------------------------------------------------------------
  
  def shutdown() { system.shutdown() }
}
