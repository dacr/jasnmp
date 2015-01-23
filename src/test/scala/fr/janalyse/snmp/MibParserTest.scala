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
import java.io._
import java.net.URL
import collection.JavaConversions._

import org.jsmiparser.parser._
import org.jsmiparser.util.url.ClassPathURLListFactory

import sys.process._

@RunWith(classOf[JUnitRunner])
class MibParserTest extends FunSuite with ShouldMatchers {

  def listMibs(directory: File): List[URL] = {
    val content = directory.listFiles.toList
    val files = content.filter(_.isFile()).map(_.toURI().toURL())
    val subfiles = content.filter(_.isDirectory()).flatMap(listMibs)
    files ::: subfiles
  }

  test("read mibs") {
    val parser = {
      val tmp = new SmiDefaultParser()
      val inputs = listMibs(new File("./mibs"))
      tmp.getFileParserPhase().setInputUrls(inputs)
      tmp
    }
    val mibs = parser.parse()
    val oid = "1.3.6.1.2.1.1.3"
    val res = mibs.findByOid(oid.split("[.]").map(_.toInt):_*)
    info("processed OID : "+ oid)
    info("Single value codeId : " + res.getSingleValue().getCodeId)
    info("values mapped with codeId : "+res.getValues.map(_.getCodeId).mkString("."))
    //val res2 = mibs.getSymbols.find("1.3.6.1.2.1.1.3")
    //info(res2.getCodeId)
  }
  
}
