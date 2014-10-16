------------------------------------------------------------------
JASNMP - JANALYSE-SNMP - SCALA SNMP API
Crosson David - crosson.david@gmail.com
------------------------------------------------------------------

------------------------------------------------------------------
Preparing a test platform:

 - Gentoo : (http://wiki.gentoo.org/wiki/SNMP_Access)
   + emerge --ask net-snmp
   + vi /etc/snmpd/snmpd.conf
      |com2sec local     127.0.0.1/32          my-own-SNMP-community
      |com2sec local     10.255.255.0/24       my-own-SNMP-community
      |#
      |group MyROGroup v1         local
      |group MyROGroup v2c        local
      |group MyROGroup usm        local
      |view all    included  .1                               80
      |access MyROGroup "" any     noauth    exact  all    none   none
      |#
      |syslocation London
      |syscontact Admin {Admin@example.com}
   + /etc/init.d/snmpd start
   + netstat -tulpen | grep 161
   + snmpwalk -v2c -c my-own-SNMP-community localhost

------------------------------------------------------------------
