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

package org.apache.jmeter.protocol.mqttws.client;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.protocol.mqttws.control.gui.MQTTPublisherGui;
import org.apache.jmeter.protocol.mqttws.control.gui.MQTTSubscriberGui;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;




import io.inventit.dev.mqtt.paho.MqttWebSocketAsyncClient;

public class MqttPublisher extends AbstractJavaSamplerClient implements Serializable, MqttCallback {
	private static final long serialVersionUID = 1L;
	private MqttWebSocketAsyncClient client;
	public static int numSeq=0;
	private AtomicInteger total = new AtomicInteger(0);
	String myname = this.getClass().getName();
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
		System.out.println(myname + ">>>> in setupTest");
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
	}

	
	public SampleResult runTest(JavaSamplerContext context) {
		
		SampleResult result = new SampleResult();
		
		if (!client.isConnected() ) {
			System.out.println(myname + " >>>> Client is not connected - Aborting test");
			result.setSuccessful(false);
			return result;
		}
		result.sampleStart(); // start stopwatch
		try {
			produce(context);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		result.sampleEnd(); 
		System.out.println(myname + ">>>> ending runTest");
		return result;
	
	}


	public void close(JavaSamplerContext context) {
		
		
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
	
	
private void produce(JavaSamplerContext context) throws Exception {
		
		// ---------------------Type of message -------------------//

		if ("FIXED".equals(context.getParameter("TYPE_MESSAGE"))) {
			System.out.println("Fixed - TODO");
			/*
			produce(context.getParameter("MESSAGE"),
					context.getParameter("TOPIC"),
					Integer.parseInt(context.getParameter("AGGREGATE")),
					context.getParameter("QOS"),
					context.getParameter("RETAINED"),
					context.getParameter("TIME_STAMP"),
					context.getParameter("NUMBER_SEQUENCE"),					
					context.getParameter("TYPE_VALUE"),
					context.getParameter("FORMAT"),
					context.getParameter("CHARSET"),
					context.getParameter("LIST_TOPIC"),
					context.getParameter("STRATEGY"),
					context.getParameter("PER_TOPIC"));
*/
		} else if ("RANDOM".equals(context.getParameter("TYPE_MESSAGE"))) {

			System.out.println("Randomly - TODO");
			/*produceRandomly(context.getParameter("SEED"),context.getParameter("MIN_RANDOM_VALUE"),
					context.getParameter("MAX_RANDOM_VALUE"),context.getParameter("TYPE_RANDOM_VALUE"),
					context.getParameter("TOPIC"),Integer.parseInt(context.getParameter("AGGREGATE")),
					context.getParameter("QOS"),context.getParameter("RETAINED"),
					context.getParameter("TIME_STAMP"),context.getParameter("NUMBER_SEQUENCE"),
					context.getParameter("TYPE_VALUE"),
					context.getParameter("FORMAT"),
					context.getParameter("CHARSET"),
					context.getParameter("LIST_TOPIC"),
					context.getParameter("STRATEGY"),
					context.getParameter("PER_TOPIC"));
					*/
									
		} else if ("TEXT".equals(context.getParameter("TYPE_MESSAGE"))) {
			produce(context.getParameter("MESSAGE"),
					context.getParameter("TOPIC"),
					Integer.parseInt(context.getParameter("AGGREGATE")),
					context.getParameter("QOS"),
					context.getParameter("RETAINED"),
					context.getParameter("TIME_STAMP"),
					context.getParameter("NUMBER_SEQUENCE"),					
					context.getParameter("TYPE_VALUE"),
					context.getParameter("FORMAT"),
					context.getParameter("CHARSET"),
					//context.getParameter("LIST_TOPIC"),
					"FALSE",
					context.getParameter("STRATEGY"),
					context.getParameter("PER_TOPIC"));
		} else if("BYTE_ARRAY".equals(context.getParameter("TYPE_MESSAGE"))){
			System.out.println("Byte Array - TODO");
			/*
			produceBigVolume(
					context.getParameter("TOPIC"),
					Integer.parseInt(context.getParameter("AGGREGATE")),
					context.getParameter("QOS"),
					context.getParameter("RETAINED"),
					context.getParameter("TIME_STAMP"),
					context.getParameter("NUMBER_SEQUENCE"),					
					context.getParameter("FORMAT"),
					context.getParameter("CHARSET"),
					context.getParameter("SIZE_ARRAY"),
					context.getParameter("LIST_TOPIC"),
					context.getParameter("STRATEGY"),
					context.getParameter("PER_TOPIC"));			
					*/
		}

	}



	



	private void produce(String message, String topic, int aggregate,
			String qos, String isRetained, String useTimeStamp, String useNumberSeq,String type_value, String format, String charset,String isListTopic,String strategy,String isPerTopic) throws Exception {
		System.out.println(myname + ">>>> Starting publishing: ");
		try {
			// Quality
			int quality = 0;
			if (MQTTPublisherGui.EXACTLY_ONCE.equals(qos)) {
				quality = 0;
			} else if (MQTTPublisherGui.AT_LEAST_ONCE.equals(qos)) {
				quality = 1;
			} else if (MQTTPublisherGui.AT_MOST_ONCE.equals(qos)) {
				quality = 2;
			}
			// Retained
			boolean retained = false;
			if ("TRUE".equals(isRetained))
				retained = true;
			//TODO send next one to GUI
			boolean throttle = true;
			// List topic
			if("FALSE".equals(isListTopic)){		
				for (int i = 0; i < aggregate; ++i) {
					byte[] payload = createPayload(message, useTimeStamp, useNumberSeq, type_value,format, charset);
					//if (quality!=0) {
					if (throttle) {
						Thread.sleep(100);
					}
					this.client.publish(topic,payload,quality,retained);
					System.out.print("*");
					total.incrementAndGet();
				}
				System.out.println("");
			} 						
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().warn(e.getLocalizedMessage(), e);
		}
	}
	
	public byte[] createPayload(String message, String useTimeStamp, String useNumSeq ,String type_value, String format, String charset) throws IOException, NumberFormatException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(b);
// flags  	
    	byte flags=0x00;
		if("TRUE".equals(useTimeStamp)) flags|=0x80;
		if("TRUE".equals(useNumSeq)) flags|=0x40;
		if (MQTTPublisherGui.INT.equals(type_value)) flags|=0x20;
		if (MQTTPublisherGui.LONG.equals(type_value)) flags|=0x10;
		if (MQTTPublisherGui.FLOAT.equals(type_value)) flags|=0x08;
		if (MQTTPublisherGui.DOUBLE.equals(type_value)) flags|=0x04;
		if (MQTTPublisherGui.STRING.equals(type_value)) flags|=0x02;
		if(!"TEXT".equals(type_value)){
			d.writeByte(flags); 
		}		
// TimeStamp
		if("TRUE".equals(useTimeStamp)){
   		 Date date= new java.util.Date();
    	 d.writeLong(date.getTime());
    	                               }
// Number Sequence
		if("TRUE".equals(useNumSeq)){
   	     d.writeInt(numSeq++);   	
   	    
  	    }
// Value				
  	    if (MQTTPublisherGui.INT.equals(type_value)) {
  			d.writeInt(Integer.parseInt(message));  			
  		} else if (MQTTPublisherGui.LONG.equals(type_value)) {
  			d.writeLong(Long.parseLong(message));  		
  		} else if (MQTTPublisherGui.DOUBLE.equals(type_value)) {
  			d.writeDouble(Double.parseDouble(message));  		
  		} else if (MQTTPublisherGui.FLOAT.equals(type_value)) {
  			d.writeDouble(Float.parseFloat(message));  			
  		} else if (MQTTPublisherGui.STRING.equals(type_value)) {
  			d.write(message.getBytes());  			
  		} else if ("TEXT".equals(type_value)) {
  			d.write(message.getBytes());
  		}   
  	      
// Format: Encoding  	   
  	   if(MQTTPublisherGui.BINARY.equals(format)){
  		   BinaryCodec encoder= new BinaryCodec();
  		   return encoder.encode(b.toByteArray());
  	   } else if(MQTTPublisherGui.BASE64.equals(format)){
  		   return Base64.encodeBase64(b.toByteArray());
  	   } else if(MQTTPublisherGui.BINHEX.equals(format)){
  		   Hex encoder= new Hex();
  		   return encoder.encode(b.toByteArray());
  	   } else if(MQTTPublisherGui.PLAIN_TEXT.equals(format)){  		  
  		   String s= new String (b.toByteArray(),charset);
  		   return s.getBytes();
  		   
  	   } else return b.toByteArray();
	}
       

}
