package com.lamoop.competition;

import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IDefenceService extends IFloodlightService {
	List<String> getBlackList();
	String getFakeIP();

}
