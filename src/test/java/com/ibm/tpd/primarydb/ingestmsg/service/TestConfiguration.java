package com.ibm.tpd.primarydb.ingestmsg.service;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cassandra.core.cql.CqlTemplate;

import com.datastax.driver.core.Session;
import com.ibm.tpd.primarydb.logger.PDBLogger;
import com.ibm.tpd.utils.PopulateValDTOWithAUIData;
import com.ibm.tpd.utils.PopulateValDTOWithUPUIData;

@Configuration
public class TestConfiguration {

	@Bean
	@Primary
	CqlTemplate cqlTemplate() {
		//Cluster cluster = new Cluster.Builder().addContactPoints("127.0.0.1").withPort(9142).build();
		  //Session session = cluster.connect();
		   return new CqlTemplate(Mockito.mock(Session.class));
		  //return new CqlTemplate(session);
	}
	
	@Bean
	@Primary
	PDBLogger pdbLogger() {
		return Mockito.mock(PDBLogger.class);
	}
	
	@Bean
	@Primary
	PopulateValDTOWithUPUIData populateValDTOWithUPUIData() {
		return Mockito.mock(PopulateValDTOWithUPUIData.class, Mockito.RETURNS_DEEP_STUBS);
	}
	
	@Bean
	@Primary
	PopulateValDTOWithAUIData populateValDTOWithAUIData() {
		return Mockito.mock(PopulateValDTOWithAUIData.class, Mockito.RETURNS_DEEP_STUBS);
	}
	
}
