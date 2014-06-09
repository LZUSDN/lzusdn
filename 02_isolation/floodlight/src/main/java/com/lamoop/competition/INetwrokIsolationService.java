package com.lamoop.competition;

import java.util.Map;

import net.floodlightcontroller.core.module.IFloodlightService;


public interface INetwrokIsolationService extends IFloodlightService {
	Map<Long, String> getMacToName();
	void enable(boolean enabled);
}
