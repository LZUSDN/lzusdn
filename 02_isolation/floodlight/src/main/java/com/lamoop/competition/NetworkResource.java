package com.lamoop.competition;

import java.util.Map;

import net.floodlightcontroller.util.MACAddress;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkResource extends ServerResource {

	@Get("json")
    public Map<Long, String> retrieve() {
		INetwrokIsolationService netIsolation = (INetwrokIsolationService)getContext().
				getAttributes().get(INetwrokIsolationService.class.getCanonicalName());
		return netIsolation.getMacToName();
	}
	
	@Post
	public String addMac(String postData){
		ObjectMapper mapper = new ObjectMapper();
		MacNetPair macNetPair = null;
		try {
			macNetPair = mapper.readValue(postData, MacNetPair.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(macNetPair != null){
			MACAddress macAddr = MACAddress.valueOf(macNetPair.mac);
			INetwrokIsolationService netIsolation = (INetwrokIsolationService)getContext().
					getAttributes().get(INetwrokIsolationService.class.getCanonicalName());
			netIsolation.getMacToName().put(macAddr.toLong(), macNetPair.net);
			return "added entity ok";
		} else {
			return "failed to add entity";
		}
	}

	@Delete
	public String delMac(String postData){
		ObjectMapper mapper = new ObjectMapper();
		MacNetPair macNetPair = null;
		try {
			macNetPair = mapper.readValue(postData, MacNetPair.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(macNetPair != null){
			MACAddress macAddr = MACAddress.valueOf(macNetPair.mac);
			INetwrokIsolationService netIsolation = (INetwrokIsolationService)getContext().
					getAttributes().get(INetwrokIsolationService.class.getCanonicalName());
			netIsolation.getMacToName().remove(macAddr.toLong());
			return "delete entity ok";
		} else {
			return "failed to delete entity";
		}
	}
}
class MacNetPair{
	public String mac;
	public String net;
}



