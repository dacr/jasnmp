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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.snmp4j.smi._

import sys.process._

/**
 * 
 * Tests requirements :
 *  - linux host
 *  - net-snmp installed
 *  - /etc/snmpd/snmpd.conf well configured
 *  - /etc/init.d/snmpd start
 *  - snmpwalk command must be available
 *  
 * Some interesting links :
 *  - http://www.oid-info.com/index.htm
 *  - http://www.alvestrand.no/objectid/1.3.6.1.2.1.html
 *  
 * More notes :
 *  - Use "snmpconf -g basic_setup" for some help in the configuration process
 */

@RunWith(classOf[JUnitRunner])
class SNMPAPITest extends FunSuite with ShouldMatchers {

  def howLongFor[T](what: => T) = {
    val begin = System.currentTimeMillis()
    val result = what
    val end = System.currentTimeMillis()
    (end - begin, result)
  }

  val host = "127.0.0.1"
  //val community = "my-own-SNMP-community"
  val community = "reader"
  
  // ---------------------------------------------------------------------------
  test("get uptime from OID") {
    // {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1) system(1) sysUpTime(3) sysUpTimeInstance(0)}
    val id= "1.3.6.1.2.1.1.3.0"
    val result = "snmpget -v2c -c %s %s %s".format(community, host, id)!!
    val snmp = new SNMP(host=host)
    val uptime = snmp.get(id, community)
    info(uptime.toString)
    info(result)
    uptime should not equal(None)
    result.contains(uptime.get.toString.split("[.]").head.split(":").init.mkString(":")) should equal(true)
  }
    
  // ---------------------------------------------------------------------------
  test("walk from MIB-2 System OID") {
    // {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1) system(1)}
    val id = "1.3.6.1.2.1.1" //  SNMP MIB-2 System
    val result = "snmpwalk -v2c -c %s %s %s".format(community, host, id)!!
    val count = result.trim.split("\n").size
    val snmp = new SNMP(host=host)
    val vbs = snmp.walk(id, community)
    for (vb <- vbs) {
       info(vb.getOid() + ":=" + vb.getVariable().toString())
    }
    vbs.size should equal(count)
  }
    
  // ---------------------------------------------------------------------------
  
}
