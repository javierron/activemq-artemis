/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.management.impl;

import javax.jms.InvalidSelectorException;
import javax.management.MBeanInfo;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQInvalidFilterExpressionException;
import org.apache.activemq.artemis.api.core.FilterConstants;
import org.apache.activemq.artemis.api.core.management.MessageCounterInfo;
import org.apache.activemq.artemis.api.core.management.Operation;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.jms.management.JMSQueueControl;
import org.apache.activemq.artemis.core.management.impl.MBeanInfoHelper;
import org.apache.activemq.artemis.core.messagecounter.MessageCounter;
import org.apache.activemq.artemis.core.messagecounter.impl.MessageCounterHelper;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;
import org.apache.activemq.artemis.jms.client.SelectorTranslator;
import org.apache.activemq.artemis.jms.management.impl.openmbean.JMSOpenTypeSupport;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.utils.json.JSONArray;
import org.apache.activemq.artemis.utils.json.JSONObject;

public class JMSQueueControlImpl extends StandardMBean implements JMSQueueControl {

   private final ActiveMQDestination managedQueue;

   private final JMSServerManager jmsServerManager;

   private final QueueControl coreQueueControl;

   private final MessageCounter counter;

   // Static --------------------------------------------------------

   /**
    * Returns null if the string is null or empty
    */
   public static String createFilterFromJMSSelector(final String selectorStr) throws ActiveMQException {
      return selectorStr == null || selectorStr.trim().length() == 0 ? null : SelectorTranslator.convertToActiveMQFilterString(selectorStr);
   }

   private static String createFilterForJMSMessageID(final String jmsMessageID) throws Exception {
      return FilterConstants.ACTIVEMQ_USERID + " = '" + jmsMessageID + "'";
   }

   static String toJSON(final Map<String, Object>[] messages) {
      JSONArray array = new JSONArray();
      for (Map<String, Object> message : messages) {
         array.put(new JSONObject(message));
      }
      return array.toString();
   }

   // Constructors --------------------------------------------------

   public JMSQueueControlImpl(final ActiveMQDestination managedQueue,
                              final QueueControl coreQueueControl,
                              final JMSServerManager jmsServerManager,
                              final MessageCounter counter) throws Exception {
      super(JMSQueueControl.class);
      this.managedQueue = managedQueue;
      this.jmsServerManager = jmsServerManager;
      this.coreQueueControl = coreQueueControl;
      this.counter = counter;
   }

   // Public --------------------------------------------------------

   // ManagedJMSQueueMBean implementation ---------------------------

   @Override
   public String getName() {
      return managedQueue.getName();
   }

   @Override
   public String getAddress() {
      return managedQueue.getAddress();
   }

   @Override
   public boolean isTemporary() {
      return managedQueue.isTemporary();
   }

   @Override
   public long getMessageCount() {
      return coreQueueControl.getMessageCount();
   }

   @Override
   public long getMessagesAdded() {
      return coreQueueControl.getMessagesAdded();
   }

   @Override
   public int getConsumerCount() {
      return coreQueueControl.getConsumerCount();
   }

   @Override
   public int getDeliveringCount() {
      return coreQueueControl.getDeliveringCount();
   }

   @Override
   public long getScheduledCount() {
      return coreQueueControl.getScheduledCount();
   }

   public boolean isDurable() {
      return coreQueueControl.isDurable();
   }

   @Override
   public String getDeadLetterAddress() {
      return coreQueueControl.getDeadLetterAddress();
   }

   @Override
   public String getExpiryAddress() {
      return coreQueueControl.getExpiryAddress();
   }

   @Override
   public String getFirstMessageAsJSON() throws Exception {
      return coreQueueControl.getFirstMessageAsJSON();
   }

   @Override
   public Long getFirstMessageTimestamp() throws Exception {
      return coreQueueControl.getFirstMessageTimestamp();
   }

   @Override
   public Long getFirstMessageAge() throws Exception {
      return coreQueueControl.getFirstMessageAge();
   }

   @Override
   public void addBinding(String binding) throws Exception {
      jmsServerManager.addQueueToBindingRegistry(managedQueue.getName(), binding);
   }

   @Override
   public String[] getRegistryBindings() {
      return jmsServerManager.getBindingsOnQueue(managedQueue.getName());
   }

   @Override
   public boolean removeMessage(final String messageID) throws Exception {
      String filter = JMSQueueControlImpl.createFilterForJMSMessageID(messageID);
      int removed = coreQueueControl.removeMessages(filter);
      if (removed != 1) {
         throw new IllegalArgumentException("No message found for JMSMessageID: " + messageID);
      }
      return true;
   }

   @Override
   public int removeMessages(final String filterStr) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      return coreQueueControl.removeMessages(filter);
   }

   @Override
   public Map<String, Object>[] listMessages(final String filterStr) throws Exception {
      try {
         String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
         Map<String, Object>[] coreMessages = coreQueueControl.listMessages(filter);

         return toJMSMap(coreMessages);
      }
      catch (ActiveMQException e) {
         throw new IllegalStateException(e.getMessage());
      }
   }

   private Map<String, Object>[] toJMSMap(Map<String, Object>[] coreMessages) {
      Map<String, Object>[] jmsMessages = new Map[coreMessages.length];

      int i = 0;

      for (Map<String, Object> coreMessage : coreMessages) {
         Map<String, Object> jmsMessage = ActiveMQMessage.coreMaptoJMSMap(coreMessage);
         jmsMessages[i++] = jmsMessage;
      }
      return jmsMessages;
   }

   private CompositeData toJMSCompositeType(CompositeDataSupport data) throws Exception {
      return JMSOpenTypeSupport.convert(data);
   }

   @Override
   public Map<String, Object>[] listScheduledMessages() throws Exception {
      Map<String, Object>[] coreMessages = coreQueueControl.listScheduledMessages();

      return toJMSMap(coreMessages);
   }

   @Override
   public String listScheduledMessagesAsJSON() throws Exception {
      return coreQueueControl.listScheduledMessagesAsJSON();
   }

   @Override
   public Map<String, Map<String, Object>[]> listDeliveringMessages() throws Exception {
      try {
         Map<String, Map<String, Object>[]> returnMap = new HashMap<>();

         // the workingMap from the queue-control
         Map<String, Map<String, Object>[]> workingMap = coreQueueControl.listDeliveringMessages();

         for (Map.Entry<String, Map<String, Object>[]> entry : workingMap.entrySet()) {
            returnMap.put(entry.getKey(), toJMSMap(entry.getValue()));
         }

         return returnMap;
      }
      catch (ActiveMQException e) {
         throw new IllegalStateException(e.getMessage());
      }
   }

   @Override
   public String listDeliveringMessagesAsJSON() throws Exception {
      return coreQueueControl.listDeliveringMessagesAsJSON();
   }

   @Override
   public String listMessagesAsJSON(final String filter) throws Exception {
      return JMSQueueControlImpl.toJSON(listMessages(filter));
   }

   @Override
   public long countMessages(final String filterStr) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      return coreQueueControl.countMessages(filter);
   }

   @Override
   public boolean expireMessage(final String messageID) throws Exception {
      String filter = JMSQueueControlImpl.createFilterForJMSMessageID(messageID);
      int expired = coreQueueControl.expireMessages(filter);
      if (expired != 1) {
         throw new IllegalArgumentException("No message found for JMSMessageID: " + messageID);
      }
      return true;
   }

   @Override
   public int expireMessages(final String filterStr) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      return coreQueueControl.expireMessages(filter);
   }

   @Override
   public boolean sendMessageToDeadLetterAddress(final String messageID) throws Exception {
      String filter = JMSQueueControlImpl.createFilterForJMSMessageID(messageID);
      int dead = coreQueueControl.sendMessagesToDeadLetterAddress(filter);
      if (dead != 1) {
         throw new IllegalArgumentException("No message found for JMSMessageID: " + messageID);
      }
      return true;
   }

   @Override
   public int sendMessagesToDeadLetterAddress(final String filterStr) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      return coreQueueControl.sendMessagesToDeadLetterAddress(filter);
   }

   @Override
   public boolean changeMessagePriority(final String messageID, final int newPriority) throws Exception {
      String filter = JMSQueueControlImpl.createFilterForJMSMessageID(messageID);
      int changed = coreQueueControl.changeMessagesPriority(filter, newPriority);
      if (changed != 1) {
         throw new IllegalArgumentException("No message found for JMSMessageID: " + messageID);
      }
      return true;
   }

   @Override
   public int changeMessagesPriority(final String filterStr, final int newPriority) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      return coreQueueControl.changeMessagesPriority(filter, newPriority);
   }

   @Override
   public boolean retryMessage(final String jmsMessageID) throws Exception {

      // Figure out messageID from JMSMessageID.
      final String filter = createFilterForJMSMessageID(jmsMessageID);
      Map<String,Object>[] messages = coreQueueControl.listMessages(filter);
      if ( messages.length != 1) { // if no messages. There should not be more than one, JMSMessageID should be unique.
         return false;
      }

      final Map<String,Object> messageToRedeliver = messages[0];
      Long messageID = (Long)messageToRedeliver.get("messageID");
      return messageID != null && coreQueueControl.retryMessage(messageID);
   }

   @Override
   public int retryMessages() throws Exception {
      return coreQueueControl.retryMessages();
   }


   @Override
   public boolean moveMessage(final String messageID, final String otherQueueName) throws Exception {
      return moveMessage(messageID, otherQueueName, false);
   }

   @Override
   public boolean moveMessage(final String messageID,
                              final String otherQueueName,
                              final boolean rejectDuplicates) throws Exception {
      String filter = JMSQueueControlImpl.createFilterForJMSMessageID(messageID);
      ActiveMQDestination otherQueue = ActiveMQDestination.createQueue(otherQueueName);
      int moved = coreQueueControl.moveMessages(filter, otherQueue.getAddress(), rejectDuplicates);
      if (moved != 1) {
         throw new IllegalArgumentException("No message found for JMSMessageID: " + messageID);
      }

      return true;
   }

   @Override
   public int moveMessages(final String filterStr,
                           final String otherQueueName,
                           final boolean rejectDuplicates) throws Exception {
      String filter = JMSQueueControlImpl.createFilterFromJMSSelector(filterStr);
      ActiveMQDestination otherQueue = ActiveMQDestination.createQueue(otherQueueName);
      return coreQueueControl.moveMessages(filter, otherQueue.getAddress(), rejectDuplicates);
   }

   @Override
   public int moveMessages(final String filterStr, final String otherQueueName) throws Exception {
      return moveMessages(filterStr, otherQueueName, false);
   }

   @Override
   @Operation(desc = "List all the existent consumers on the Queue")
   public String listConsumersAsJSON() throws Exception {
      return coreQueueControl.listConsumersAsJSON();
   }

   @Override
   public String listMessageCounter() {
      try {
         return MessageCounterInfo.toJSon(counter);
      }
      catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   @Override
   public void resetMessageCounter() throws Exception {
      coreQueueControl.resetMessageCounter();
   }

   @Override
   public String listMessageCounterAsHTML() {
      return MessageCounterHelper.listMessageCounterAsHTML(new MessageCounter[]{counter});
   }

   @Override
   public String listMessageCounterHistory() throws Exception {
      return MessageCounterHelper.listMessageCounterHistory(counter);
   }

   @Override
   public String listMessageCounterHistoryAsHTML() {
      return MessageCounterHelper.listMessageCounterHistoryAsHTML(new MessageCounter[]{counter});
   }

   @Override
   public boolean isPaused() throws Exception {
      return coreQueueControl.isPaused();
   }

   @Override
   public void pause() throws Exception {
      coreQueueControl.pause();
   }

   @Override
   public void resume() throws Exception {
      coreQueueControl.resume();
   }

   @Override
   public CompositeData[] browse() throws Exception {
      return browse(null);
   }

   @Override
   public CompositeData[] browse(String filter) throws Exception {
      try {
         CompositeData[] messages =  coreQueueControl.browse(filter);

         ArrayList<CompositeData> c = new ArrayList<>();

         for (CompositeData message : messages) {
            c.add(toJMSCompositeType((CompositeDataSupport) message));
         }
         CompositeData[] rc = new CompositeData[c.size()];
         c.toArray(rc);
         return rc;
      }
      catch (ActiveMQInvalidFilterExpressionException e) {
         throw new InvalidSelectorException(e.getMessage());
      }
   }

   @Override
   public String getSelector() {
      return coreQueueControl.getFilter();
   }

   @Override
   public void flushExecutor() {
      coreQueueControl.flushExecutor();
   }

   @Override
   public MBeanInfo getMBeanInfo() {
      MBeanInfo info = super.getMBeanInfo();
      return new MBeanInfo(info.getClassName(), info.getDescription(), info.getAttributes(), info.getConstructors(), MBeanInfoHelper.getMBeanOperationsInfo(JMSQueueControl.class), info.getNotifications());
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
