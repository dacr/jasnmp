/*
 * Copyright 2012 David Crosson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.janalyse.snmp

import java.io.IOException
import org.snmp4j._
import org.snmp4j.event.ResponseEvent
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi._
import org.snmp4j.transport.DefaultTcpTransportMapping
import org.snmp4j.transport.DefaultUdpTransportMapping
import SnmpConstants.{ version1, version2c, version3 }
import collection.JavaConversions._

object SNMP {
  val v1 = version1
  val v2c = version2c
  val v3 = version3
}


/*
 * TODO : snmp.send => peut prendre en paramètre un listener... asynchronisme est géré !! A faire absolument. 
 */


class SNMP(
  host: String = "127.0.0.1",
  port: Int = 161,
  timeout: Int = 3000,
  retry: Int = 2,
  version: Int = SNMP.v2c,
  protocol: String = "UDP") {

  private def using[T <: { def close() }, R](resource: T)(block: T => R) = {
    try block(resource)
    finally resource.close
  }

  private def transport(protocol: String) = {
    if (protocol.equalsIgnoreCase("UDP")) {
      new DefaultUdpTransportMapping()
    } else {
      new DefaultTcpTransportMapping()
    }
  }

  private def target(community: String) = {
    val target = new CommunityTarget()
    target.setCommunity(new OctetString(community))
    val address = GenericAddress.parse(protocol + ":" + host + "/" + port)
    target.setAddress(address)
    target.setVersion(version)
    target.setTimeout(timeout)
    target
  }

  def get(soid: String, community: String): Option[Variable] = get(new OID(soid), community)
    
  def get(oid: OID, community: String): Option[Variable] = {
    val pdu = new PDU()
    pdu.setType(PDU.GET)
    pdu.addOID(new VariableBinding(oid))
    pdu.setNonRepeaters(0)
    using(new Snmp(transport(protocol))) { snmp =>
      snmp.listen()
      val resp = snmp.send(pdu, target(community))
      val respPDU = resp.getResponse()
      val vbs = respPDU.getVariableBindings()
      if (vbs.size > 0) {
        val vb = vbs.get(0).asInstanceOf[VariableBinding]
        Some(vb.getVariable())
      } else None
    }
  }

  def getList(oid_list: Iterable[OID], community: String): Iterable[VariableBinding] = {
    val pdu = new PDU()
    pdu.setType(PDU.GET)
    //put the oids you want to get
    /*List<VariableBinding> ivbs = new ArrayList<VariableBinding>();
            for (OID o : oid_list) {
                ivbs.add(new VariableBinding(o));
            }*/
    //pdu.addAll(ivbs.toArray(new VariableBinding[]{}));
    pdu.addAll(oid_list.map(new VariableBinding(_)).toArray)
    pdu.setMaxRepetitions(10)
    pdu.setNonRepeaters(0)

    using(new Snmp(transport(protocol))) { snmp =>
      snmp.listen()

      // send the PDU
      val responseEvent = snmp.send(pdu, target(community))
      // extract the response PDU (could be null if timed out)
      val responsePDU = responseEvent.getResponse()
      val vbs = responsePDU.getVariableBindings()
      var oidDone = Set.empty[OID]
      for (vb <- vbs.map(_.asInstanceOf[VariableBinding]) if !oidDone.contains(vb.getOid)) yield {
        oidDone += vb.getOid
        vb
        /*List<OID> rec_oid = new ArrayList<OID>();
	                for (int i = 0; i < vbs.size(); i++) {
	                    VariableBinding v = (VariableBinding) vbs.get(i);
	                    if (!rec_oid.contains(v.getOid())) {
	                        rec_oid.add(v.getOid());
	                        ret.add((VariableBinding) vbs.get(i));
	                    }
	                }*/
      }
    }
  }

  def set(oid: OID, value: Variable, community: String) {
    val pdu = new PDU()
    pdu.setType(PDU.SET)
    pdu.add(new VariableBinding(oid, value))
    pdu.setNonRepeaters(0)

    using(new Snmp(transport(protocol))) { snmp =>
      snmp.listen()

      val resp = snmp.set(pdu, target(community))
      Option(resp.getResponse()) match {
        case None => println("SNMP Timeout occured.")
        case Some(respPDU) =>
          val vbs = respPDU.getVariableBindings()
          if (vbs.size() > 0) {
            val vb = vbs.get(0).asInstanceOf[VariableBinding]
            !vb.isException()
          } else false
      }
    }
  }

 
  def walk(soid: String, community: String): Iterable[VariableBinding] = walk(new OID(soid), community)
  
  def walk(oid: OID, community: String): Iterable[VariableBinding] = {
    val requestPDU = new PDU()
    requestPDU.add(new VariableBinding(oid))
    requestPDU.setType(PDU.GETNEXT)

    using(new Snmp(transport(protocol))) { snmp =>
      //transport.listen()
      snmp.listen()

      var finished = false
      var ret = Vector[VariableBinding]()

      while (!finished) {
        var vb: VariableBinding = null;

        val respEvt = snmp.send(requestPDU, target(community))
        Option(respEvt.getResponse()) match {
          case Some(respPDU) if respPDU.size>0 =>
            vb = respPDU.get(0)
            if (respPDU.getErrorStatus() != 0) finished = true
            else if (vb.getOid() == null) finished = true
            else if (vb.getOid().size() < oid.size()) finished = true
            else if (oid.leftMostCompare(oid.size(), vb.getOid()) != 0) finished = true
            else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) finished = true
            else if (vb.getOid().compareTo(oid) <= 0) finished = true
            else {
              ret :+= vb
              // Set up the variable binding for the next entry.
              requestPDU.setRequestID(new Integer32(0));
              requestPDU.set(0, vb);
            }
          case None => finished = true
          case _ => // TODO - empty response ?
        }
      }
      ret
    }
  }

  def getNext(oid: OID, community: String): Option[VariableBinding] = {

    val pdu = new PDU()
    pdu.setType(PDU.GETNEXT)
    //put the oid you want to get
    pdu.add(new VariableBinding(oid));
    pdu.setNonRepeaters(0);

    using(new Snmp(transport(protocol))) { snmp =>
      snmp.listen()

      // send the PDU
      val responseEvent = snmp.send(pdu, target(community))
      Option(responseEvent.getResponse()) match {
        case null => None // could be timed out
        case Some(respPDU) if respPDU.size > 0 =>
          Some(respPDU.getVariableBindings().get(0).asInstanceOf[VariableBinding])
        case _ => None
      }
    }

  }

}
