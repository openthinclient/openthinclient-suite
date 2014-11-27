

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
import javax.naming.directory.InitialDirContext;

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
import org.openthinclient.dhcp.DhcpService;

@RunWith(Arquillian.class)
public class TcatDhcpStoreTest {

	private static final Logger logger = Logger.getLogger(TcatDhcpStoreTest.class);
	
	// TODO: Muss über Properties geholt werden: identisch mit Wert in jboss-service.xml
	private static final short ldapPort = 10389;
	
	protected static String baseDN = "dc=test,dc=test";
	
	@Deployment
	public static EnterpriseArchive createDeployment() {
		
		PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");
		File[] protocolDhcp = pom.resolve("org.openthinclient.3rd-party.apacheds:apacheds-protocol-dhcp").withTransitivity().asFile();
		File[] otcCommon = pom.resolve("org.openthinclient:common").withTransitivity().asFile();
		File[] apacheDs = pom.resolve("org.openthinclient.3rd-party.apacheds:apacheds-server-sar:sar:1.0.1").withoutTransitivity().asFile();
		File[] xerces = pom.resolve("xerces:xercesImpl").withTransitivity().asFile();
		
		JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "sar-dhcp.sar")
				.addPackage(DhcpService.class.getPackage())
				.addAsResource("META-INF/jboss-service.xml")
				.addAsResource("META-INF/config-xmbean.xml")
				.addAsResource("META-INF/jboss-deployment-structure.xml")
				.addAsResource("client.xml")
				.addClass(TcatDhcpStoreTest.class)
				;
				
		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
		ear.addAsModule(sar);
		ear.addAsModules(apacheDs);
		ear.addAsLibraries(protocolDhcp);
		ear.addAsLibraries(otcCommon);
		ear.addAsLibraries(xerces);

		logger.info("created deployment: " + ear.toString(true));
		return ear;  					 
	}
	
	
	/**
     * Test that the partition has been correctly created
     */
	@Test
    public void testPartition() throws NamingException
    {
        getContext();

    }
	
	protected DirContext getContext() throws NamingException {
		final Hashtable env = new Hashtable();
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		// env.put( Context.INITIAL_CONTEXT_FACTORY,
		// DEFAULT_INITIAL_CONTEXT_FACTORY );
		env.put(Context.PROVIDER_URL, "ldap://localhost:10389/dc=tcat,dc=test");

		return new InitialDirContext(env);
	}

    
}
