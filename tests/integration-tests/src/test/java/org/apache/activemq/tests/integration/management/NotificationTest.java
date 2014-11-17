/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.apache.activemq.tests.integration.management;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.api.core.ActiveMQException;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.api.core.client.ClientConsumer;
import org.apache.activemq.api.core.client.ClientMessage;
import org.apache.activemq.api.core.client.ClientSession;
import org.apache.activemq.api.core.client.ClientSessionFactory;
import org.apache.activemq.api.core.client.HornetQClient;
import org.apache.activemq.api.core.client.ServerLocator;
import org.apache.activemq.api.core.management.ManagementHelper;
import org.apache.activemq.core.client.impl.ClientSessionInternal;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.core.server.HornetQServer;
import org.apache.activemq.core.server.HornetQServers;
import org.apache.activemq.tests.util.RandomUtil;
import org.apache.activemq.tests.util.UnitTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.activemq.api.core.management.CoreNotificationType.BINDING_ADDED;
import static org.apache.activemq.api.core.management.CoreNotificationType.BINDING_REMOVED;
import static org.apache.activemq.api.core.management.CoreNotificationType.CONSUMER_CLOSED;
import static org.apache.activemq.api.core.management.CoreNotificationType.CONSUMER_CREATED;

/**
 * A NotificationTest
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class NotificationTest extends UnitTestCase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private HornetQServer service;

   private ClientSession session;

   private ClientConsumer notifConsumer;

   private SimpleString notifQueue;
   private ServerLocator locator;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   @Test
   public void testBINDING_ADDED() throws Exception
   {
      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      NotificationTest.flush(notifConsumer);

      session.createQueue(address, queue, durable);

      ClientMessage[] notifications = NotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(BINDING_ADDED.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(queue.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ROUTING_NAME)
         .toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
         .toString());

      session.deleteQueue(queue);
   }

   @Test
   public void testBINDING_ADDEDWithMatchingFilter() throws Exception
   {
      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      System.out.println(queue);
      notifConsumer.close();
      notifConsumer = session.createConsumer(notifQueue.toString(), ManagementHelper.HDR_ROUTING_NAME + "= '" +
         queue +
         "'");
      NotificationTest.flush(notifConsumer);

      session.createQueue(address, queue, durable);

      ClientMessage[] notifications = NotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(BINDING_ADDED.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(queue.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ROUTING_NAME)
         .toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
         .toString());

      session.deleteQueue(queue);
   }

   @Test
   public void testBINDING_ADDEDWithNonMatchingFilter() throws Exception
   {
      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      System.out.println(queue);
      notifConsumer.close();
      notifConsumer = session.createConsumer(notifQueue.toString(), ManagementHelper.HDR_ROUTING_NAME + " <> '" +
         queue +
         "'");
      NotificationTest.flush(notifConsumer);

      session.createQueue(address, queue, durable);

      NotificationTest.consumeMessages(0, notifConsumer);

      session.deleteQueue(queue);
   }

   @Test
   public void testBINDING_REMOVED() throws Exception
   {
      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      session.createQueue(address, queue, durable);

      NotificationTest.flush(notifConsumer);

      session.deleteQueue(queue);

      ClientMessage[] notifications = NotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(BINDING_REMOVED.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(queue.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ROUTING_NAME)
         .toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
         .toString());
   }

   @Test
   public void testCONSUMER_CREATED() throws Exception
   {
      ClientSessionFactory sf = createSessionFactory(locator);
      ClientSession mySession = sf.createSession("myUser",
                                                 "myPassword",
                                                 false,
                                                 true,
                                                 true,
                                                 locator.isPreAcknowledge(),
                                                 locator.getAckBatchSize());

      mySession.start();

      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      session.createQueue(address, queue, durable);

      NotificationTest.flush(notifConsumer);

      ClientConsumer consumer = mySession.createConsumer(queue);
      SimpleString consumerName = SimpleString.toSimpleString(((ClientSessionInternal) mySession).getName());

      ClientMessage[] notifications = NotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(CONSUMER_CREATED.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(queue.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ROUTING_NAME)
         .toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
         .toString());
      Assert.assertEquals(1, notifications[0].getObjectProperty(ManagementHelper.HDR_CONSUMER_COUNT));
      Assert.assertEquals(SimpleString.toSimpleString("myUser"), notifications[0].getSimpleStringProperty(ManagementHelper.HDR_USER));
      Assert.assertEquals(SimpleString.toSimpleString("invm:0"), notifications[0].getSimpleStringProperty(ManagementHelper.HDR_REMOTE_ADDRESS));
      Assert.assertEquals(consumerName, notifications[0].getSimpleStringProperty(ManagementHelper.HDR_SESSION_NAME));

      consumer.close();
      session.deleteQueue(queue);
   }

   @Test
   public void testCONSUMER_CLOSED() throws Exception
   {
      ClientSessionFactory sf = createSessionFactory(locator);
      ClientSession mySession = sf.createSession("myUser",
                                                 "myPassword",
                                                 false,
                                                 true,
                                                 true,
                                                 locator.isPreAcknowledge(),
                                                 locator.getAckBatchSize());

      mySession.start();

      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();
      boolean durable = RandomUtil.randomBoolean();

      mySession.createQueue(address, queue, durable);
      ClientConsumer consumer = mySession.createConsumer(queue);
      SimpleString sessionName = SimpleString.toSimpleString(((ClientSessionInternal) mySession).getName());

      NotificationTest.flush(notifConsumer);

      consumer.close();

      ClientMessage[] notifications = NotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(CONSUMER_CLOSED.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(queue.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ROUTING_NAME)
         .toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
         .toString());
      Assert.assertEquals(0, notifications[0].getObjectProperty(ManagementHelper.HDR_CONSUMER_COUNT));
      Assert.assertEquals(SimpleString.toSimpleString("myUser"), notifications[0].getSimpleStringProperty(ManagementHelper.HDR_USER));
      Assert.assertEquals(SimpleString.toSimpleString("invm:0"), notifications[0].getSimpleStringProperty(ManagementHelper.HDR_REMOTE_ADDRESS));
      Assert.assertEquals(sessionName, notifications[0].getSimpleStringProperty(ManagementHelper.HDR_SESSION_NAME));

      session.deleteQueue(queue);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      Configuration conf = createBasicConfig()
         // the notifications are independent of JMX
         .addAcceptorConfiguration(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      service = HornetQServers.newHornetQServer(conf, false);
      service.start();

      locator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      ClientSessionFactory sf = createSessionFactory(locator);
      session = sf.createSession(false, true, true);
      session.start();

      notifQueue = RandomUtil.randomSimpleString();

      session.createQueue(ActiveMQDefaultConfiguration.getDefaultManagementNotificationAddress(), notifQueue, null, false);

      notifConsumer = session.createConsumer(notifQueue);
   }

   @Override
   @After
   public void tearDown() throws Exception
   {
      notifConsumer.close();

      session.deleteQueue(notifQueue);
      session.close();

      if (locator != null)
      {
         locator.close();
      }

      service.stop();

      super.tearDown();
   }

   // Private -------------------------------------------------------

   private static void flush(final ClientConsumer notifConsumer) throws ActiveMQException
   {
      ClientMessage message = null;
      do
      {
         message = notifConsumer.receive(500);
      }
      while (message != null);
   }

   protected static ClientMessage[] consumeMessages(final int expected, final ClientConsumer consumer) throws Exception
   {
      ClientMessage[] messages = new ClientMessage[expected];

      ClientMessage m = null;
      for (int i = 0; i < expected; i++)
      {
         m = consumer.receive(500);
         if (m != null)
         {
            for (SimpleString key : m.getPropertyNames())
            {
               System.out.println(key + "=" + m.getObjectProperty(key));
            }
         }
         Assert.assertNotNull("expected to received " + expected + " messages, got only " + i, m);
         messages[i] = m;
         m.acknowledge();
      }
      m = consumer.receiveImmediate();
      if (m != null)
      {
         for (SimpleString key : m.getPropertyNames())

         {
            System.out.println(key + "=" + m.getObjectProperty(key));
         }
      }
      Assert.assertNull("received one more message than expected (" + expected + ")", m);

      return messages;
   }

   // Inner classes -------------------------------------------------

}
