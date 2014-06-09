package com.lamoop.competition;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.packet.DNS;
import net.floodlightcontroller.packet.DNSAnswer;
import net.floodlightcontroller.packet.DNSQuery;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.util.OFMessageDamper;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

public class Defence implements IOFMessageListener, IFloodlightModule,
		IDefenceService {
	protected IFloodlightProviderService floodlightProvider;
	protected Logger logger;
	protected IRestApiService restApi;
	protected List<String> blackList;
	protected List<String> defenceList;
	protected String fakeIP = "192.168.1.12";
	protected ICounterStoreService counterStore;
	protected OFMessageDamper messageDamper;
	protected static int OFMESSAGE_DAMPER_CAPACITY = 10000; // ms.
	protected static int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms
	boolean enabled;

	@Override
	public String getName() {
		return "defence";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
	}

	@Override
	public List<String> getBlackList() {
		return blackList;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IDefenceService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IDefenceService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(Defence.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		counterStore = context.getServiceImpl(ICounterStoreService.class);
		messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY,
				EnumSet.of(OFType.FLOW_MOD), OFMESSAGE_DAMPER_TIMEOUT);
		blackList = new ArrayList<String>();
		defenceList = new ArrayList<String>();
		enabled = true;
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		Map<String, String> configOptions = context.getConfigParams(this);
		String domains = configOptions.get("blackList");
		Iterable<String> items = Splitter.on(',').trimResults()
				.omitEmptyStrings().split(domains);
		for (String item : items) {
			blackList.add(item);
		}
		domains = configOptions.get("defenceList");
		items = Splitter.on(',').trimResults()
				.omitEmptyStrings().split(domains);
		for (String item : items) {
			defenceList.add(item);
		}
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Command result = Command.CONTINUE;
		switch (msg.getType()) {
		case PACKET_IN:
			result = processPacketInMessage(sw, msg, cntx);
			break;
		default:
			break;
		}
		return result;
	}

	private Command processPacketInMessage(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		if (cntx != null) {
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
					IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			IPacket pkt = null;
			try {
				pkt = eth.getPayload().getPayload().getPayload();
			} catch (Exception e) {
				logger.info("it is not a dns packet");
			}
			if (pkt instanceof DNS) {
				return processDNSMessage(sw, msg, cntx);
			}
		}
		return Command.CONTINUE;
	}

	private Command processDNSMessage(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		logger.info("process dns packet.");
		OFPacketIn pi = (OFPacketIn) msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPv4 nwPkt = (IPv4) eth.getPayload();
		UDP tlPkt = (UDP) nwPkt.getPayload();
		DNS pkt = (DNS) tlPkt.getPayload();
		List<DNSQuery> querys = pkt.getQuerys();
		List<DNSAnswer> answers = new ArrayList<DNSAnswer>();
		String domain;
		InetAddress domainIP = null;
		for (DNSQuery query : querys) {
			domain = query.getName();
			DNSAnswer answer = new DNSAnswer();
			if (domainVerify(domain)) {
				logger.info("the domain is not verified.");
				answer.setAddr(fakeIP);
				answers.add(answer);
			} else {
				try {
					domainIP = InetAddress.getByName(domain);
				} catch (UnknownHostException e) {
					logger.error("can not get the ip address of " + domain);
				}
				if (domainIP != null) {
					answer.setAddr(domainIP.getHostAddress());
					answers.add(answer);
				}
			}
		}
		IPacket dns = new DNS().setTid(pkt.getTid()).setFlags((short) 0x8180)
				.setQuestionCount((short) querys.size())
				.setAnswerCount((short) answers.size())
				.setQuerys(querys)
				.setAnswers(answers);
		IPacket udp = new UDP().setDestinationPort(tlPkt.getSourcePort())
				.setSourcePort(tlPkt.getDestinationPort())
				.setPayload(dns);
		IPacket ipv4 = new IPv4().setDiffServ((byte) 0)
				.setIdentification((short) 3358).setFlags((byte) 0)
				.setFragmentOffset((short) 0).setTtl((byte) 62)
				.setProtocol(IPv4.PROTOCOL_UDP)
				.setSourceAddress(nwPkt.getDestinationAddress())
				.setDestinationAddress(nwPkt.getSourceAddress())
				.setPayload(udp);
		IPacket dnsReply = new Ethernet()
				.setSourceMACAddress(eth.getDestinationMACAddress())
				.setDestinationMACAddress(eth.getSourceMACAddress())
				.setEtherType(Ethernet.TYPE_IPv4).setVlanID(eth.getVlanID())
				.setPriorityCode(eth.getPriorityCode())
				.setPayload(ipv4);
		pushPacket(dnsReply, sw, OFPacketOut.BUFFER_ID_NONE,
				OFPort.OFPP_NONE.getValue(), pi.getInPort(), cntx, true);
		return Command.STOP;
	}

	public void pushPacket(IPacket packet, IOFSwitch sw, int bufferId,
			short inPort, short outPort, FloodlightContext cntx, boolean flush) {
		logger.trace("PacketOut srcSwitch={} inPort={} outPort={}",
				new Object[] { sw, inPort, outPort });

		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);

		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outPort, (short) 0xffff));

		po.setActions(actions).setActionsLength(
				(short) OFActionOutput.MINIMUM_LENGTH);
		short poLength = (short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);

		// set buffer_id, in_port
		po.setBufferId(bufferId);
		po.setInPort(inPort);

		// set data - only if buffer_id == -1
		if (po.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
			if (packet == null) {
				logger.error("BufferId is not set and packet data is null. "
						+ "Cannot send packetOut. "
						+ "srcSwitch={} inPort={} outPort={}", new Object[] {
						sw, inPort, outPort });
				return;
			}
			byte[] packetData = packet.serialize();
			poLength += packetData.length;
			po.setPacketData(packetData);
		}

		po.setLength(poLength);

		try {
			counterStore.updatePktOutFMCounterStoreLocal(sw, po);
			messageDamper.write(sw, po, cntx, flush);
		} catch (IOException e) {
			logger.error("Failure writing packet out", e);
		}
	}
	
	private boolean domainVerify(String domain){
		if(blackList.contains(domain)){
			return true;
		} else{
			for(String item:defenceList){
				if(StringComparer.compare(domain, item)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getFakeIP() {
		return fakeIP;
	}
}
