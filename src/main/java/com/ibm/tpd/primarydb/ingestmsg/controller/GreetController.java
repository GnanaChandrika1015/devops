package com.ibm.tpd.primarydb.ingestmsg.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* @author Sangita Pal
*
*/

@RestController
public class GreetController {
	
    @RequestMapping("/greeting")
    public String greet() {
    	
        return "Hello from dispatch-msg-ingest-service !!";
    }
}