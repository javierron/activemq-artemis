/**
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
package org.apache.activemq.core.protocol.core.impl.wireformat;

import org.apache.activemq.api.core.ActiveMQBuffer;
import org.apache.activemq.api.core.Pair;
import org.apache.activemq.api.core.TransportConfiguration;

public class ClusterTopologyChangeMessage_V3 extends ClusterTopologyChangeMessage_V2
{
   private String scaleDownGroupName;

   public ClusterTopologyChangeMessage_V3(final long uniqueEventID, final String nodeID, final String backupGroupName, final String scaleDownGroupName,
                                          final Pair<TransportConfiguration, TransportConfiguration> pair, final boolean last)
   {
      super(CLUSTER_TOPOLOGY_V3);

      this.nodeID = nodeID;

      this.pair = pair;

      this.last = last;

      this.exit = false;

      this.uniqueEventID = uniqueEventID;

      this.backupGroupName = backupGroupName;

      this.scaleDownGroupName = scaleDownGroupName;
   }

   public ClusterTopologyChangeMessage_V3()
   {
      super(CLUSTER_TOPOLOGY_V3);
   }

   public String getScaleDownGroupName()
   {
      return scaleDownGroupName;
   }

   @Override
   public void encodeRest(final ActiveMQBuffer buffer)
   {
      super.encodeRest(buffer);
      buffer.writeNullableString(scaleDownGroupName);
   }

   @Override
   public void decodeRest(final ActiveMQBuffer buffer)
   {
      super.decodeRest(buffer);
      scaleDownGroupName = buffer.readNullableString();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((scaleDownGroupName == null) ? 0 : scaleDownGroupName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (!super.equals(obj))
      {
         return false;
      }
      if (!(obj instanceof ClusterTopologyChangeMessage_V3))
      {
         return false;
      }
      ClusterTopologyChangeMessage_V3 other = (ClusterTopologyChangeMessage_V3) obj;
      if (scaleDownGroupName == null)
      {
         if (other.scaleDownGroupName != null)
         {
            return false;
         }
      }
      else if (!scaleDownGroupName.equals(other.scaleDownGroupName))
      {
         return false;
      }
      return true;
   }
}
