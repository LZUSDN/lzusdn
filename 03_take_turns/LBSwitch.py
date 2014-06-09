# Copyright (C) 2011 Nippon Telegraph and Telephone Corporation.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
import time

from ryu.base import app_manager
#from ryu.controller import macToPort
from ryu.controller import ofp_event
from ryu.controller.handler import MAIN_DISPATCHER, DEAD_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0
from ryu.lib.mac import haddr_to_bin
from ryu.lib.ip import ipv4_to_bin
from ryu.lib.packet import packet, ethernet, ipv4, arp
from ryu.lib import hub
from ryu.ofproto import ether


def ipv4_to_int(string):
    ip = string.split('.')
    assert len(ip) == 4
    i = 0
    for b in ip:
        b = int(b)
        i = (i << 8) | b
    return i


class SimpleSwitch(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(SimpleSwitch, self).__init__(*args, **kwargs)
        self.macToPort = {}
        #self.ipToMac = {'192.168.1.11': 'd4:be:d9:a6:77:b3','192.168.1.21':'b8:ca:3a:73:1d:bb','192.168.1.22':'b8:ca:3a:73:1d:07'}
        self.ipToMac = {}
        self.cookie = 13           # cookie for flow modify
        self.idleTimeout = 5       # idle timeout for flow modify
        self.logger.setLevel(logging.INFO)
        self.macTimeout = 30       # mac address timeout for forward table
        self.macCheckPeriod = 3    # the period of check forward table
        self.pktThreshold = 0.005  # min time for the same packet

        #self.vips = {'10.0.0.100':['10.0.0.1', '10.0.0.2', '10.0.0.3']}
        self.vips = {'192.168.1.100':['192.168.1.21', '192.168.1.22']}
        self.memberIndex = 0
        self.pktCheck = {}
        self.pktInHandler = [self._loadbalancer, self._forwarding]
        #self.macCheckThread = hub.spawn(self.macCheck)

        self.vipMembers = []
        for vip in self.vips:
            self.vipMembers.extend(self.vips[vip])

    def macCheck(self):
        while  True:
            expiredMac = []
            now = time.time()
            for dpid in self.macToPort:
                for mac in self.macToPort[dpid]:
                    if now - self.macToPort[dpid][mac]['time'] > self.macTimeout:
                        expiredMac.append({'dpid':dpid, 'mac':mac})

            for dpMac in expiredMac:
                dpid = dpMac['dpid']
                mac = dpMac['mac']
                self.logger.info("delete mac:%s on %s", mac, dpid)
                del self.macToPort[dpid][mac]
            hub.sleep(self.macCheckPeriod)

    def addFlow(self, datapath, match, actions, idleTimeout):
        ofproto = datapath.ofproto

        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=self.cookie,
            command=ofproto.OFPFC_ADD, idle_timeout=idleTimeout, hard_timeout=0,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=actions)
        datapath.send_msg(mod)

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)  # @UndefinedVariable
    def _packetInHandler(self, ev):
        # broadcast detect
        msg = ev.msg
        datapath = msg.datapath
        dpid = msg.datapath.id
        ofproto = datapath.ofproto
        pkt = packet.Packet(msg.data)
        pktHash = hash(pkt)

        if dpid in self.pktCheck:
            if pktHash in self.pktCheck[dpid]:
                now = time.time()
                lastSeen = self.pktCheck[dpid][pktHash]
                if now - lastSeen < self.pktThreshold:
                    actions = [datapath.ofproto_parser.OFPActionOutput(ofproto.OFPP_NONE)]
                    pktOut = datapath.ofproto_parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=msg.in_port, actions=actions)
                    datapath.send_msg(pktOut)
                    return
        else:
            self.pktCheck[dpid] = {}

        self.pktCheck[dpid][pktHash] = time.time()

        # learn mac address from the packet
        eth = pkt.get_protocol(ethernet.ethernet)
        dlsrc = eth.src
        self.logger.debug('dpid:%s, src: %s, inport:%s, pkt hash:%s', dpid, dlsrc, msg.in_port, hash(pkt))
        self.macToPort.setdefault(dpid, {})
        self.macToPort[dpid][dlsrc] = {'port':msg.in_port, 'time':time.time()}

        # learn ip address from the packet
        if eth.ethertype == 0x0800:
            nw = pkt.get_protocol(ipv4.ipv4)
            self.ipToMac[nw.src] = dlsrc
            self.logger.info("nw src:%s, nw dst:%s", nw.src, nw.dst)


        # forwarding packet
        for pktInFun in self.pktInHandler:
            isContinue = pktInFun(ev)
            if not isContinue:
                break

    def _loadbalancer(self, ev):
        msg = ev.msg
        pkt = packet.Packet(msg.data)
        arpPkt = pkt.get_protocol(arp.arp)
        if arpPkt:
            if arpPkt.dst_ip in self.vips:
                self.logger.info('process arp packet for vips')
                self._replyArp(ev, arpPkt.dst_ip)
                return False
        ipPkt = pkt.get_protocol(ipv4.ipv4)
        if ipPkt:
            if ipPkt.dst in self.vips:
                self._processVipRequest(ev)
                return False
        return True

    def virtualIpToMac(self, ipAddr):
        tmp = ipAddr.replace('.', '') * 3
        res = ''
        for i in xrange(6):
            start = i * 2
            res = ':'+ tmp[start:start+2] + res
        return res[1:]

    def _replyArp(self, ev, vip):
        self.logger.info('send arp reply')

        msg = ev.msg
        datapath = msg.datapath
        port = msg.in_port
        pkt = packet.Packet(data = msg.data)
        ethPkt = pkt.get_protocol(ethernet.ethernet)
        arpPkt = pkt.get_protocol(arp.arp)
        vipMac = self.virtualIpToMac(vip)

        arpReply = packet.Packet()
        arpReply.add_protocol(ethernet.ethernet(ethertype=ethPkt.ethertype,
                                           dst=ethPkt.src,
                                           src=vipMac))
        arpReply.add_protocol(arp.arp(opcode=arp.ARP_REPLY,
                                 src_mac=vipMac,
                                 src_ip=vip,
                                 dst_mac=arpPkt.src_mac,
                                 dst_ip=arpPkt.src_ip))
        self._sendPacket(datapath, port, arpReply)

    def _processVipRequest(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        dpid = msg.datapath.id
        ofproto = datapath.ofproto
        pkt = packet.Packet(msg.data)

        eth = pkt.get_protocol(ethernet.ethernet)
        nw = pkt.get_protocol(ipv4.ipv4)
        dlsrc = eth.src
        dldst = eth.dst
        nwsrc = nw.src
        nwdst = nw.dst

        members = self.vips[nwdst]
        if len(members) > 0:
            pickedIp = members[self.memberIndex % len(members)]
            pickedMac = self.ipToMac[pickedIp]
            self.memberIndex = self.memberIndex + 1
            if pickedMac in self.macToPort[dpid]:
                out_port = self.macToPort[dpid][pickedMac]['port']
            else:
                out_port = ofproto.OFPP_FLOOD
            # flow for request
            actions = [datapath.ofproto_parser.OFPActionSetDlDst(haddr_to_bin(pickedMac)),
                       datapath.ofproto_parser.OFPActionSetNwDst(ipv4_to_int(pickedIp)),
                       datapath.ofproto_parser.OFPActionOutput(out_port)]
            match = datapath.ofproto_parser.OFPMatch(dl_src=haddr_to_bin(dlsrc),
                                                     dl_dst=haddr_to_bin(dldst))
            if out_port != ofproto.OFPP_FLOOD:
                self.addFlow(datapath, match, actions, 1)
            # flow for response
            actions2 = [datapath.ofproto_parser.OFPActionSetDlSrc(haddr_to_bin(dldst)),
                       datapath.ofproto_parser.OFPActionSetNwSrc(ipv4_to_int(nwdst)),
                       datapath.ofproto_parser.OFPActionOutput(msg.in_port)]
            match2 = datapath.ofproto_parser.OFPMatch(dl_src=haddr_to_bin(pickedMac),
                                                     dl_dst=haddr_to_bin(dlsrc))
            self.addFlow(datapath, match2, actions2, 1)

            out = datapath.ofproto_parser.OFPPacketOut(
                datapath=datapath, buffer_id=msg.buffer_id, in_port=msg.in_port,
                actions=actions)
            datapath.send_msg(out)
        return False


    def _forwarding(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        dpid = datapath.id
        ofproto = datapath.ofproto

        pkt = packet.Packet(msg.data)
        self.logger.debug('dpid:%s, pkt hash:%s', datapath.id, hash(pkt))
        eth = pkt.get_protocol(ethernet.ethernet)

        dst = eth.dst
        if dst in self.macToPort[dpid]:
            out_port = self.macToPort[dpid][dst]['port']
        else:
            out_port = ofproto.OFPP_FLOOD

        match = datapath.ofproto_parser.OFPMatch(in_port=msg.in_port, dl_dst=haddr_to_bin(dst))
        actions = [datapath.ofproto_parser.OFPActionOutput(out_port)]
        # install a flow to avoid packet_in next time
        if out_port != ofproto.OFPP_FLOOD:
            self.addFlow(datapath, match, actions, self.idleTimeout)

        out = datapath.ofproto_parser.OFPPacketOut(
            datapath=datapath, buffer_id=msg.buffer_id, in_port=msg.in_port,
            actions=actions)
        datapath.send_msg(out)

        return True

    @set_ev_cls(ofp_event.EventOFPStateChange, DEAD_DISPATCHER)
    def _stateChangeHandler(self, ev):
        dpid = ev.datapath.id
        if dpid in self.macToPort:
            self.logger.debug('delete datapath: %s', dpid)
            del self.macToPort[dpid]
            del self.ipToMac[dpid]

    def _sendPacket(self, datapath, port, pkt):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        pkt.serialize()
        self.logger.info("packet-out %s", pkt)
        data = pkt.data
        actions = [parser.OFPActionOutput(port=port)]
        out = parser.OFPPacketOut(datapath=datapath,
                                  buffer_id=ofproto.OFP_NO_BUFFER,
                                  in_port=ofproto.OFPP_CONTROLLER,
                                  actions=actions,
                                  data=data)
        datapath.send_msg(out)



