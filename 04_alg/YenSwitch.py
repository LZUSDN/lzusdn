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
from ryu.topology.api import get_switch, get_link

from yen.graph import DiGraph
from yen import algorithms

class SwitchPort:
    def __init__(self, s, p):
        self.switch = s
        self.port = p
        self.time = time.time()

class SimpleSwitch(app_manager.RyuApp):

    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(SimpleSwitch, self).__init__(*args, **kwargs)

        self.mac2port = {}
        self.cookie = 13           # cookie for flow modify
        self.idleTimeout = 5       # idle timeout for flow modify
        self.logger.setLevel(logging.INFO)
        self.pktThreshold = 0.005  # min time for the same packet
        self.pktCheck = {}
        self.pktInHandler = [self._deviceManager, self._forwarding]
        self.devices = {}


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

        # forwarding packet
        for pktInFun in self.pktInHandler:
            isContinue = pktInFun(ev)
            if not isContinue:
                break
            
    def _deviceManager(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        dpid = msg.datapath.id
        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocol(ethernet.ethernet)
        dlsrc = eth.src
        if not dlsrc in self.devices:
            self.devices[dlsrc] = SwitchPort(dpid, msg.in_port)
        return True;

    def _forwarding(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        dpid = datapath.id
        ofproto = datapath.ofproto

        pkt = packet.Packet(msg.data)
        self.logger.debug('dpid:%s, pkt hash:%s', datapath.id, hash(pkt))
        eth = pkt.get_protocol(ethernet.ethernet)

        dst = eth.dst
        src = eth.src

        if dst in self.devices:
            srcSwitchPort = self.devices[src]
            dstSwitchPort = self.devices[dst]
            route = self.getRoute(srcSwitchPort, dstSwitchPort)
            match = datapath.ofproto_parser.OFPMatch(dl_src=haddr_to_bin(src), dl_dst=haddr_to_bin(dst))
            self.pushFlow(self, route, match, self.idleTimeout, dst)
        else:
            out_port = ofproto.OFPP_FLOOD
            actions = [datapath.ofproto_parser.OFPActionOutput(out_port)]
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

    def addFlow(self, datapath, match, actions, idleTimeout):
        ofproto = datapath.ofproto

        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=self.cookie,
            command=ofproto.OFPFC_ADD, idle_timeout=idleTimeout, hard_timeout=0,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=actions)
        datapath.send_msg(mod)

    def pushFlow(self, route, match, idleTimeout, dst):
        path = route['path']

        for i in xrange(len(path) - 1):
            out_port = self.getLinkPort(path[i], path[i+1])
            datapath = self.findDp(path[i])
            actions = [datapath.ofproto_parser.OFPActionOutput(out_port)]
            self.addFlow(datapath, match, actions, idleTimeout)

        out_port = self.devices[dst].port
        datapath = self.findDp(path[-1])
        actions = [datapath.ofproto_parser.OFPActionOutput(out_port)]
        self.addFlow(datapath, match, actions, idleTimeout)

    def getRoute(self, src, dst):
        G = DiGraph()
        G._data  =  self.makeGraph()
        items = algorithms.ksp_yen(G, src, dst, 3)
        return items[0]

    def makeGraph(self):
        g = {}
        links = get_link(self)
        switches = get_switch(self)
        for switch in switches:
            g[switch.dp.id] = {}
        for link in links:
            g[link.src.dpid][link.src.dpid] = 1
        return g

    def getLinkPort(self, srcDpid, dstDpid):
        links = get_link(self)
        for link in links:
            if link.src.dpid == srcDpid and link.dst.dpid == dstDpid:
                return link.src.port_no

    def findDp(self, dpid):
        switches = get_switch(self)
        for switch in switches:
            if switch.dp.id == dpid:
                return switch.dp



