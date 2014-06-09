package com.lamoop.competition;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class NetworkRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/network/json/", NetworkResource.class);
        return router;
	}

	@Override
	public String basePath() {
		return "/wm/networkisolation";
	}

}
