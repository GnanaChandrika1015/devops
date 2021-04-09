package com.ibm.tpd.primarydb.ingestmsg;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;

import com.ibm.tpd.TpdCommonServiceApplication;
import com.ibm.tpd.primarydb.ingestmsg.service.TPDOutboundMessageStreams;

/**
 * @author SangitaPal
 *
 */
@SpringBootApplication
@EnableBinding(TPDOutboundMessageStreams.class)
@ComponentScan({"com.ibm.tpd.primarydb.ingestmsg","com.ibm.tpd.primarydb.errorhandler"})
public class IngestMessageApplication extends TpdCommonServiceApplication{
	
	public static void main(String[] args) {
		SpringApplication.run(IngestMessageApplication.class, args);
	}
}