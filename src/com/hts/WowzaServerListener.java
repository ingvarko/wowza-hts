package com.hts;

import com.wowza.wms.logging.*;
import com.wowza.wms.server.*;

public class WowzaServerListener implements IServerNotify2 {

	public void onServerConfigLoaded(IServer server) {
		WMSLoggerFactory.getLogger(null).info("onServerConfigLoaded");
	}

	public void onServerCreate(IServer server) {
		WMSLoggerFactory.getLogger(null).info("onServerCreate");
	}

	public void onServerInit(IServer server) {
		WMSLoggerFactory.getLogger(null).info("iy--------------- onServerInit");
		new HotelManagerClient().unregisterAll();
//		System.out.println(server.getProperties());
	}

	public void onServerShutdownStart(IServer server) {
		WMSLoggerFactory.getLogger(null).info("onServerShutdownStart");
	}

	public void onServerShutdownComplete(IServer server) {
		WMSLoggerFactory.getLogger(null).info("onServerShutdownComplete");
	}

}
