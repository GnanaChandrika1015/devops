package com.ibm.tpd.primarydb.ingestmsg.service;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

/**
 * @author SangitaPal
 *
 */
@Component
public interface TPDOutboundMessageStreams {
    //Producer Operational topic channels
	String Dispatch_OUT = "dispatch_out";
	String Dispatch_DLQ_OUT = "dispatch_dlq_out";

    /*
     * Below message channel will be used to push the message to Operational topic.   
     */

	//Operational channel for message type Dispatch of tobacco products: 3-3
    @Output(Dispatch_OUT)
    MessageChannel dispatchOutboundChannel();
    
    /**
	 * Dead Letter Queue Producer channel for message type Dispatch of tobacco products: 3-3
	 */
	@Output(Dispatch_DLQ_OUT)
    MessageChannel dispatchDLQOutboundChannel();
    
}
