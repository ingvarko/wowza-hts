package com.hts;

import java.io.*;

import com.wowza.wms.http.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.vhost.*;
/**
 * http://localhost:8086/wzhttpservlet?getAllFlvs
 * @author iyuvchen
 *
 */
public class WZHttpServlet extends HTTProvider2Base {

	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp) {
		WMSLoggerFactory.getLogger(null).info("WZHttpServlet: onHTTPRequest");
		
		if (!doHTTPAuthentication(vhost, req, resp))
			return;

		String retStr = new WowzaRESTService().getFLVs();

		try {
			OutputStream out = resp.getOutputStream();
			byte[] outBytes = retStr.getBytes();
			out.write(outBytes);
		}
		catch (Exception e) {
			WMSLoggerFactory.getLogger(null).error("WZHttpServlet: " + e.toString());
		}

	}

}
