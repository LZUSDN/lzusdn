package com.lamoop.competition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkIsolation implements IOFMessageListener, IFloodlightModule, INetwrokIsolationService {

	protected IFloodlightProviderService floodlightProvider;
	protected Logger logger;
	boolean enabled;
	Map<Long, String> macToName;
	protected IRestApiService restApi;
	
	@Override
	public String getName() {
		return NetworkIsolation.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		 Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(INetwrokIsolationService.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(INetwrokIsolationService.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(IRestApiService.class);
		    return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
	    floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    logger = LoggerFactory.getLogger(NetworkIsolation.class);
	    restApi = context.getServiceImpl(IRestApiService.class);
	    macToName = new HashMap<>();
	    enabled = true;
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApi.addRestletRoutable(new NetworkRoutable());
	}

	@Override
	public Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        if (!this.enabled)
            return Command.CONTINUE;

        switch (msg.getType()) {
        case PACKET_IN:
            IRoutingDecision decision = null;
            if (cntx != null) {
                decision = IRoutingDecision.rtStore.get(cntx,
                        IRoutingDecision.CONTEXT_DECISION);

                return this.processPacketInMessage(sw, (OFPacketIn) msg,
                        decision, cntx);
            }
            break;
        default:
            break;
        }

        return Command.CONTINUE;

	}

	private Command processPacketInMessage(
			IOFSwitch sw, OFPacketIn msg, IRoutingDecision decision,
			FloodlightContext cntx) {
		logger.info("proccess packet in message");
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		Long srcMACHash = Ethernet.toLong(eth.getSourceMACAddress());
		Long dstMACHash = Ethernet.toLong(eth.getDestinationMACAddress());
		
        if(macToName.containsKey(srcMACHash) && macToName.containsKey(dstMACHash)){
        	logger.info("source mac address and destination mac address are in the map");
        	
        	String srcNetName = macToName.get(srcMACHash);
        	String dstNetName = macToName.get(dstMACHash);
        	if(!srcNetName.equals(dstNetName)){
        		logger.info("source mac and destination mac are not in the same network");
        		decision = new RoutingDecision(sw.getId(), msg.getInPort()
                		, IDeviceService.fcStore.
                        get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
                        IRoutingDecision.RoutingAction.DROP);
                decision.addToContext(cntx);
        	}
        }
        return Command.CONTINUE;
	}

	@Override
	public Map<Long, String> getMacToName() {
		return macToName;
	}

	@Override
	public void enable(boolean enabled) {
		this.enabled = enabled;
	}

}
