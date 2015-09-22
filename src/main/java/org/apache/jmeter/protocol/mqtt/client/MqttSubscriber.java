/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License. 

  Copyright 2014 University Joseph Fourier, LIG Laboratory, ERODS Team

*/

package org.apache.jmeter.protocol.mqtt.client;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.protocol.mqtt.control.gui.MQTTSubscriberGui;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.inventit.dev.mqtt.paho.MqttWebSocketAsyncClient;

public class MqttSubscriber extends AbstractJavaSamplerClient implements Serializable, MqttCallback {
	private static final long serialVersionUID = 1L;
	private MqttWebSocketAsyncClient client;
	//private MqttClient client;
	


	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument("HOST", "tcp://localhost:1883");
		defaultParameters.addArgument("CLIENT_ID", "${__time(YMDHMS)}${__threadNum}");
		defaultParameters.addArgument("TOPIC", "TEST.MQTT");
		defaultParameters.addArgument("AGGREGATE", "100");
		defaultParameters.addArgument("DURABLE", "false");
		return defaultParameters;
	}

	public void setupTest(JavaSamplerContext context){
		System.out.println("mpika setupTestttttt");
		String host = context.getParameter("HOST");
		String clientId = context.getParameter("CLIENT_ID");
		if("TRUE".equalsIgnoreCase(context.getParameter("RANDOM_SUFFIX"))){
			clientId= MqttPublisher.getClientId(clientId,Integer.parseInt(context.getParameter("SUFFIX_LENGTH")));	
		}
		try {
			System.out.println("Host: " + host + "clientID: " + clientId);
			client = new MqttWebSocketAsyncClient(host, clientId, new MemoryPersistence());
			//client = new MqttClient(host, clientId, new MemoryPersistence());
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		MqttConnectOptions options = new MqttConnectOptions();
		options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
		options.setCleanSession(true);
		/*String user = context.getParameter("USER"); 
		String pwd = context.getParameter("PASSWORD");
		boolean durable = Boolean.parseBoolean(context.getParameter("DURABLE"));
		options.setCleanSession(!durable);
		if (user != null) {
			options.setUserName(user);
			if ( pwd!=null ) {
				options.setPassword(pwd.toCharArray());
			}
		}
		*/
		//TODO more options here
		try {
			client.connect(options);
			int i=0;
			if (!client.isConnected() && (i<5) ) {
				try {
					i++;
					Thread.sleep(2000);
					System.out.println(".");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		client.setCallback(this);
		/*new MqttCallback() {

		    @Override
		    public void messageArrived(String topic, MqttMessage message)
		            throws Exception {
		    	System.out.println("hohohohohohohohohohoh");
		    }

		    @Override
		    public void deliveryComplete(IMqttDeliveryToken token) {
		    }

		    @Override
		    public void connectionLost(Throwable cause) {
		    }
		  });
		  */
	}

	
	private class EndTask extends TimerTask  {
		boolean timeup = false;
	    public void run()  {
	      System.out.println("Time's up!");
	      timeup = true;
	      }
	    public boolean isTimeUp(){
	    	return timeup;
	    }
	 }

	public SampleResult runTest(JavaSamplerContext context) {
		
		SampleResult result = new SampleResult();
		
		if (!client.isConnected() ) {
			result.setSuccessful(false);
			return result;
		}
		result.sampleStart(); // start stopwatch
		try {
			client.subscribe(context.getParameter("TOPIC"), 0);
		} catch (MqttException e) {
			System.out.println("ohohohoh");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EndTask endtask = new EndTask();
		Timer timer = new Timer();
		System.out.println("Waiting for: " + Long.parseLong(context.getParameter("TIMEOUT")));
		timer.schedule( endtask, Long.parseLong(context.getParameter("TIMEOUT")));
		//wait out for messages till TIMEOUT expires
		while ( !endtask.isTimeUp() ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		
		result.sampleEnd(); 
		System.out.println("ending runTest");
		return result;
	
	}


	public void close(JavaSamplerContext context) {
		System.out.println("mpika close");
		
	}
	
	private static final String mycharset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static String getClientId(String clientPrefix, int suffixLength) {
	    Random rand = new Random(System.nanoTime()*System.currentTimeMillis());
	    StringBuilder sb = new StringBuilder();
	    sb.append(clientPrefix);
	    for (int i = 0; i < suffixLength; i++) {
	        int pos = rand.nextInt(mycharset.length());
	        sb.append(mycharset.charAt(pos));
	    }
	    return sb.toString();
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String str, MqttMessage msg) throws Exception {
		System.out.println("got message: " + new String(msg.getPayload()));
		// TODO Auto-generated method stub
		
	}
}
