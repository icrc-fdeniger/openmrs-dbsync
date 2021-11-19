package org.openmrs.eip.dbsync.sender;

import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openmrs.eip.dbsync.model.SyncModel;
import org.openmrs.eip.dbsync.utils.JsonUtils;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

@Import(TestSenderConfig.class)
@ComponentScan("org.openmrs.eip")
@SqlGroup({ @Sql(value = "classpath:test_data.sql"), @Sql(value = "classpath:sender_test_data.sql") })
public abstract class BaseSenderTest extends BaseWatcherRouteTest {
	
	protected static GenericContainer artemisContainer = new GenericContainer("cnocorch/activemq-artemis");
	
	protected static final String CREATOR_UUID = "1a3b12d1-5c4f-415f-871b-b98a22137605";
	
	protected static final String SOURCE_SITE_ID = "test";
	
	private static final String ARTEMIS_ETC = "/var/lib/artemis/etc/";
	
	private static final String QUEUE_NAME = "sync.test.queue";
	
	protected static Integer artemisPort;
	
	private static ActiveMQConnection activeMQConn;
	
	private ActiveMQQueue queue;
	
	@BeforeClass
	public static void startArtemis() throws Exception {
		
		artemisContainer.withCopyFileToContainer(MountableFile.forClasspathResource("artemis-roles.properties"),
		    ARTEMIS_ETC + "artemis-roles.properties");
		artemisContainer.withCopyFileToContainer(MountableFile.forClasspathResource("artemis-users.properties"),
		    ARTEMIS_ETC + "artemis-users.properties");
		artemisContainer.withCopyFileToContainer(MountableFile.forClasspathResource("broker.xml"),
		    ARTEMIS_ETC + "broker.xml");
		
		Startables.deepStart(Stream.of(artemisContainer)).join();
		artemisPort = artemisContainer.getMappedPort(61616);
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory("tcp://localhost:" + artemisPort);
		activeMQConn = (ActiveMQConnection) connFactory.createConnection("admin", "admin");
		activeMQConn.start();
	}
	
	@Before
	public void beforeBaseSenderTestMethod() throws Exception {
		if (queue != null) {
			activeMQConn.destroyDestination(queue);
		}
	}
	
	public void fireEvent(String table, String databaseId, String identifier, String op) {
		producerTemplate.sendBodyAndProperty("direct:sender-db-sync", null, PROP_EVENT,
		    createEvent(table, databaseId, identifier, op));
	}
	
	public List<SyncModel> getSyncMessagesInQueue() throws Exception {
		List<SyncModel> syncMessages = new ArrayList();
		try (Session session = activeMQConn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
			if (queue == null) {
				queue = (ActiveMQQueue) session.createQueue(QUEUE_NAME);
			}
			
			try (QueueBrowser browser = session.createBrowser(queue)) {
				Enumeration messages = browser.getEnumeration();
				while (messages.hasMoreElements()) {
					String m = ((TextMessage) messages.nextElement()).getText();
					syncMessages.add(JsonUtils.unmarshalSyncModel(m));
				}
			}
		}
		
		return syncMessages;
	}
	
}
