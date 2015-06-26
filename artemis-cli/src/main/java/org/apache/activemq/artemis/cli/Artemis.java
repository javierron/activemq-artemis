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
package org.apache.activemq.artemis.cli;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import io.airlift.airline.Cli;
import org.apache.activemq.artemis.cli.commands.Action;
import org.apache.activemq.artemis.cli.commands.ActionContext;
import org.apache.activemq.artemis.cli.commands.Browse;
import org.apache.activemq.artemis.cli.commands.Consumer;
import org.apache.activemq.artemis.cli.commands.Create;
import org.apache.activemq.artemis.cli.commands.HelpAction;
import org.apache.activemq.artemis.cli.commands.Kill;
import org.apache.activemq.artemis.cli.commands.Producer;
import org.apache.activemq.artemis.cli.commands.Run;
import org.apache.activemq.artemis.cli.commands.Stop;
import org.apache.activemq.artemis.cli.commands.tools.CompactJournal;
import org.apache.activemq.artemis.cli.commands.tools.DecodeJournal;
import org.apache.activemq.artemis.cli.commands.tools.EncodeJournal;
import org.apache.activemq.artemis.cli.commands.tools.HelpData;
import org.apache.activemq.artemis.cli.commands.tools.PrintData;
import org.apache.activemq.artemis.cli.commands.tools.XmlDataExporter;
import org.apache.activemq.artemis.cli.commands.tools.XmlDataImporter;

public class Artemis
{
   @SuppressWarnings("unchecked")
   public static void main(String... args) throws Exception
   {
      try
      {
         execute(args);
      }
      catch (ConfigurationException configException)
      {
         System.err.println(configException.getMessage());
         System.out.println();
         System.out.println("Configuration should be specified as 'scheme:location'. Default configuration is 'xml:${ARTEMIS_INSTANCE}/etc/bootstrap.xml'");
      }
      catch (RuntimeException re)
      {
         System.err.println(re.getMessage());
         System.out.println();

         Cli<Action> parser = builder(null).build();

         parser.parse("help").execute(ActionContext.system());
      }
   }

   public static Object execute(String... args) throws Exception
   {
      return execute(null, null, args);
   }

   public static Object execute(File artemisHome, File artemisInstance, String... args) throws Exception
   {
      Action action = builder(artemisInstance).build().parse(args);
      action.setHomeValues(artemisHome, artemisInstance);

      if (action.isVerbose())
      {
         System.out.print("Executing " + action.getClass().getName() + " ");
         for (String arg : args)
         {
            System.out.print(arg + " ");
         }
         System.out.println();
         System.out.println("Home::" + action.getBrokerHome() + ", Instance::" + action.getBrokerInstance());
      }

      return action.execute(ActionContext.system());
   }

   private static Cli.CliBuilder<Action> builder(File artemisInstance)
   {
      String instance = artemisInstance != null ? artemisInstance.getAbsolutePath() : System.getProperty("artemis.instance");
      Cli.CliBuilder<Action> builder = Cli.<Action>builder("artemis")
         .withDescription("ActiveMQ Artemis Command Line")
         .withCommand(HelpAction.class)
         .withCommand(Producer.class)
         .withCommand(Consumer.class)
         .withCommand(Browse.class)
         .withDefaultCommand(HelpAction.class);


      builder.withGroup("data")
         .withDescription("data tools group (print|exp|imp|exp|encode|decode|compact) (example ./artemis data print)").
         withDefaultCommand(HelpData.class).withCommands(PrintData.class, XmlDataExporter.class,
                                                         XmlDataImporter.class, DecodeJournal.class, EncodeJournal.class,
                                                         CompactJournal.class);

      if (instance != null)
      {
         builder = builder.withCommands(Run.class, Stop.class, Kill.class);
      }
      else
      {
         builder = builder.withCommand(Create.class);
      }

      return builder;
   }


   public static void printBanner() throws Exception
   {
      copy(Artemis.class.getResourceAsStream("banner.txt"), System.out);
   }

   private static long copy(InputStream in, OutputStream out) throws Exception
   {
      byte[] buffer = new byte[1024];
      int len = in.read(buffer);
      while (len != -1)
      {
         out.write(buffer, 0, len);
         len = in.read(buffer);
      }
      return len;
   }

}
