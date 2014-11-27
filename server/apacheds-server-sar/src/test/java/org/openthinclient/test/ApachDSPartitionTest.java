package org.openthinclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.sar.DirectoryService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ApachDSPartitionTest {

	private static final Logger logger = Logger.getLogger(ApachDSPartitionTest.class);
	
	// TODO: Muss über Properties geholt werden: identisch mit Wert in jboss-service.xml
	private static final short ldapPort = 10389;
	
	protected static String baseDN = "dc=test,dc=test";
	
	@Deployment
	public static EnterpriseArchive createDeployment() {
		
		PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");
		File[] adssjndi = pom.resolve("org.openthinclient.3rd-party.apacheds:apacheds-server-jndi").withTransitivity().asFile();
		
		JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "sar-apacheds.sar")
				.addPackage(DirectoryService.class.getPackage())
				.addAsResource("META-INF/jboss-service.xml")
				.addAsResource("META-INF/jboss-deployment-structure.xml")
				.addClass(ApachDSPartitionTest.class)
				;
				
		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
		ear.addAsModule(sar);
		ear.addAsLibraries(adssjndi);

		logger.info("created deployment: " + ear.toString(true));
		return ear;  					 
	}
	
	
	/**
     * Test that the partition has been correctly created
     */
	@Test
    public void testPartition() throws NamingException
    {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();

        // Create a new context pointing to the 'uid=admin,ou=system' partition
        env.put( Context.PROVIDER_URL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.jndi.ServerContextFactory" );

        // Let's open a connection on this partition
        InitialContext initialContext = new InitialContext( env );

        // We should be able to read it
        DirContext appRoot = ( DirContext ) initialContext.lookup( "" );
        assertNotNull( appRoot );

        // Let's get the entry associated to the top level
        Attributes attributes = appRoot.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "admin", attributes.get( "uid" ).get() );

        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        System.out.println(attribute);
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalPerson" ) );
        assertTrue( attribute.contains( "inetOrgPerson" ) );
        assertTrue( attribute.contains( "person" ) );

    }
    
}
