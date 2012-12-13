package com.hts;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sf.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.client.IClient;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.module.ModuleCore;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify2;

public class HotelManagerClient extends ModuleBase {

	private static final String HOTELMANAGERURL = "hotel.manager.url";
	// will be overwritten in play()
	private static String hotelManagerUrl = "http://127.0.0.1:9080/hotel-manager/rest/json/broadcast";

	
	class StreamListener implements IMediaStreamActionNotify2 {
		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket) {
//			System.out.println("onMetaData[" + stream.getContextStr() + "]: " + metaDataPacket.toString());
		}

		public void onPauseRaw(IMediaStream stream, boolean isPause, double location) {
//			System.out.println("onPauseRaw[" + stream.getContextStr() + "]: isPause:" + isPause + " location:"
//					+ location);
		}

		public void onPause(IMediaStream stream, boolean isPause, double location) {
//			System.out.println("onPause[" + stream.getContextStr() + "]: isPause:" + isPause + " location:" + location);
		}

		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) {
//			System.out.println("onPlay[" + stream.getContextStr() + "]: playStart:" + playStart + " playLen:" + playLen
//					+ " playReset:" + playReset);
		}

		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
			getLogger().info("onPublish[" + stream.getContextStr() + "]: isRecord:" + isRecord + " isAppend:"
					+ isAppend);
			new HotelManagerClient().create(streamName);

		}

		public void onSeek(IMediaStream stream, double location) {
//			System.out.println("onSeek[" + stream.getContextStr() + "]: location:" + location);
		}

		public void onStop(IMediaStream stream) {
//			System.out.println("onStop[" + stream.getContextStr() + "]: ");
		}

		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
			getLogger().info("onUnPublish[" + stream.getContextStr() + "]: streamName:" + streamName + " isRecord:"
					+ isRecord + " isAppend:" + isAppend);
	
			new HotelManagerClient().unregister(streamName);
		}
	}

	@SuppressWarnings("unchecked")
	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("onStreamCreate[" + stream.getName() + "]: clientId:" + stream.getClientId());
		IMediaStreamActionNotify2 actionNotify = new StreamListener();

		WMSProperties props = stream.getProperties();
		synchronized (props) {
			props.put("streamActionNotifier", actionNotify);
		}
		stream.addClientListener(actionNotify);
	}

	public void onStreamDestroy(IMediaStream stream) {
		getLogger().info("onStreamDestroy[" + stream.getName() + "]: clientId:" + stream.getClientId());

		IMediaStreamActionNotify2 actionNotify = null;
		WMSProperties props = stream.getProperties();
		synchronized (props) {
			actionNotify = (IMediaStreamActionNotify2) stream.getProperties().get("streamActionNotifier");
		}
		if (actionNotify != null) {
			stream.removeClientListener(actionNotify);
		}
	}

	public void play(IClient client, RequestFunction function, AMFDataList params) {
//		getLogger().info("Play event");
		String videoname = getParamString(params, PARAM1);
		String ipAddress = client.getIp();
		boolean status = checkAccess(videoname, ipAddress);

		if (status)
			ModuleCore.play(client, function, params);
		else {
			client.rejectConnection("some error");
			ModuleCore.closeStream(client, function, params);
			ModuleCore.deleteStream(client, function, params);
		}
	}

	public void onAppStart(IApplicationInstance appInstance) {
//		getLogger().info("onAppStart: " + appInstance.getApplication().getName() + "/" + appInstance.getName());
		hotelManagerUrl = (String) appInstance.getProperties().get(HOTELMANAGERURL);
		unregisterAll();
	}

	public void onAppStop(IApplicationInstance appInstance) {
		// getLogger().info("onAppStop: " + appInstance.getApplication().getName() + "/" + appInstance.getName());
	}

	public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
		// getLogger().info("onConnect: " + client.getClientId());
//		getLogger().info("onConnect Client IP: " + client.getIp());
		// getLogger().info("Client stream: " + getStream(client, function));
		// getLogger().info("sreams: " + client.getPlayStreams());
	}

	public void onDisconnect(IClient client) {
		// getLogger().info("onDisconnect: " + client.getClientId());
	}

	public void onConnectAccept(IClient client) {
		// getLogger().info("onConnectAccept: " + client.getClientId());
		// getLogger().info("onAccept Client get Play streams: " + client.getPlayStreams());
		// getLogger().info("onAccept Client get Publish streams: " + client.getPublishStreams());
	}

	public void onConnectReject(IClient client) {
		// getLogger().info("onConnectReject: " + client.getClientId());
	}

	public Boolean checkAccess(String broadcastStreamName, String ipAddress) {
		Client client = Client.create();

		getLogger().info("Checking access for " + ipAddress + " to " + broadcastStreamName);

		WebResource webResource = null;
		try {
			webResource = client
					.resource(getBaseURL(broadcastStreamName) + "/" + URLEncoder.encode(ipAddress, "UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			getLogger().error(e.getMessage());
			return null;
		}

		//TODO if the access to hotel-manager unavailable put into FIFO queue
		ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

		int status = response.getStatus();
		switch (status) {
		case 201:
			getLogger().info("Access allowed to " + broadcastStreamName + " for " + ipAddress);
			return true;
		case 403:
			getLogger().info("Access not allowed to " + broadcastStreamName + " for " + ipAddress);
			return false;
		default:
			getLogger().error("Failed : HTTP error code : " + status);
			throw new RuntimeException("Failed : HTTP error code : " + status);
		}

	}

	/**
	 * 
	 * @param stringToEncode - using URLEncoder.encode(stringToEncode, "UTF-8");
	 * @return BASEURL +"/"+ URLEncoder.encode(stringToEncode, "UTF-8");
	 * @throws UnsupportedEncodingException
	 */
	public String getBaseURL(String stringToEncode) throws UnsupportedEncodingException {
		return hotelManagerUrl + "/" + URLEncoder.encode(stringToEncode, "UTF-8");
	}

	public String unregisterAll() {
		WebResource webResource = null;

		Client client = Client.create();

		try {
			webResource = client.resource(getBaseURL(""));
		}
		catch (UnsupportedEncodingException e) {
			getLogger().error(e.getMessage());
			return null;
		}
		ClientResponse response = null;
		try {
			response = webResource.delete(ClientResponse.class);
		}
		catch (Exception e) {
			// TODO If hotel-manager is unavailable put all operations into a FIFO queue
			getLogger().error("Warning: RMI to hotel-manager failed");
			getLogger().error(e.getMessage());
			return null;
		}

		if (response.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);
		getLogger().info(output);
		return output;

	}

	public String create(String broadcastStreamName) {
		Client client = Client.create();

		WebResource webResource = null;
		try {
			webResource = client.resource(getBaseURL("put"));
		}
		catch (UnsupportedEncodingException e) {
			getLogger().error(e.getMessage());
			return null;
		}

		JSONObject map = new JSONObject();
		map.put("streamName", broadcastStreamName);
		map.put("active", true); // not necessary. Set true in BroadcasStream constructor;

		String input = map.toString();

		ClientResponse response = null;

		try {
			response = webResource.type("application/json").put(ClientResponse.class, input);
		}
		catch (Exception e) {
			// TODO If hotel-manager is unavailable put all operations into a FIFO queue
			getLogger().error("Warning: RMI to hotel-manager failed");
			getLogger().error(e.getMessage());
			return null;
		}

		if (response.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);
		getLogger().info("[WS] BroadcastStreamClient: " + output);

		return output;
	}

	public String unregister(String broadcastStreamName) {
		WebResource webResource = null;

		Client client = Client.create();

		try {
			webResource = client.resource(getBaseURL(broadcastStreamName));
		}
		catch (UnsupportedEncodingException e) {
			getLogger().error(e.getMessage());
			return null;
		}
		ClientResponse response = null;
		try {
			response = webResource.delete(ClientResponse.class);
		}
		catch (Exception e) {
			// TODO If hotel-manager is unavailable put all operations into a FIFO queue
			getLogger().error("Warning: RMI to hotel-manager failed");
			getLogger().error(e.getMessage());
			return null;
		}

		if (response.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);
		getLogger().info(output);
		return output;

	}

	public static void main(String[] args) {
		new HotelManagerClient().unregisterAll();
		new HotelManagerClient().create("sample.mp4");
		new HotelManagerClient().checkAccess("sample.mp4", "127.0.0.1");
		new HotelManagerClient().unregister("sample.mp4");
	}

}
