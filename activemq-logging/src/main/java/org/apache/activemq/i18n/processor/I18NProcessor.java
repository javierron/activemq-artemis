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

package org.apache.activemq.i18n.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import org.apache.activemq.i18n.annotation.Bundle;
import org.apache.activemq.i18n.annotation.LogMessage;
import org.apache.activemq.i18n.annotation.Message;

@SupportedAnnotationTypes({"org.apache.activemq.i18n.annotation.Bundle"})
public class I18NProcessor extends AbstractProcessor
{

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
   {
      //     * Valuable tips found at http://www.zdnet.com/article/writing-and-processing-custom-annotations-part-3/

      System.out.println("annotations: " + annotations);

      try
      {

         for (TypeElement annotation : annotations)
         {
            System.out.println("Annotation: " + annotation);
            for (Element annotatedTypeEl : roundEnv.getElementsAnnotatedWith(annotation))
            {

               TypeElement annotatedType = (TypeElement) annotatedTypeEl;

               Bundle bundleAnnotation = annotatedType.getAnnotation(Bundle.class);


               String fullClassName = annotatedType.getQualifiedName() + "_impl";
               String interfaceName = annotatedType.getSimpleName().toString();
               String simpleClassName = interfaceName + "_impl";
               PrintWriter writerOutput = new PrintWriter(processingEnv.getFiler().createSourceFile(fullClassName).openWriter());

               // header
               writerOutput.println("/** This class is auto generated by " + I18NProcessor.class.getCanonicalName());
               writerOutput.println("    and it inherits whatever license is declared at " + annotatedType + " */");
               writerOutput.println();

               // opening package
               writerOutput.println("package " + annotatedType.getEnclosingElement() + ";");
               writerOutput.println();

               // Opening class
               writerOutput.println("// " + bundleAnnotation.toString());
               writerOutput.println("public class " + simpleClassName + " implements " + interfaceName);
               writerOutput.println("{");

               // Declaring the static field that's used by {@link I18NFactory}
               writerOutput.println("   public static " + simpleClassName + " INSTANCE = new " + simpleClassName + "();");
               writerOutput.println();

               for (Element el : annotatedType.getEnclosedElements())
               {
                  if (el.getKind() == ElementKind.METHOD)
                  {

                     ExecutableElement executableMember = (ExecutableElement) el;

                     Message messageAnnotation = el.getAnnotation(Message.class);
                     LogMessage logAnnotation = el.getAnnotation(LogMessage.class);
                     if (messageAnnotation != null && logAnnotation != null)
                     {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't use both Message and Log at a method at " + el);
                        return false;
                     }

                     if (messageAnnotation != null)
                     {
                        generateMessage(bundleAnnotation, writerOutput, executableMember, messageAnnotation);
                     }
                     else if (logAnnotation != null)
                     {
                        generateLogger(bundleAnnotation, writerOutput, executableMember, logAnnotation);
                     }
                     else
                     {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation not found at " + el);
                        return false;
                     }


                  }
               }

               // temporary hack until we find a better way to log
               writerOutput.println("    // temporary hack until we figure out logging");
               writerOutput.println("    public boolean isTraceEnabled() { return false; }");
               writerOutput.println("    public boolean isDebugEnabled() { return false ; } ");
               writerOutput.println("    public void debug(String msg) { System.out.println(msg); }");
               writerOutput.println("    public void debug(String msg, Throwable e) { Exception e2 = new Exception (msg); e2.initCause(e); e.printStackTrace();}");
               writerOutput.println("    public void info (String msg) { System.out.println(msg); }");
               writerOutput.println("    public void info(String msg, Throwable e) { Exception e2 = new Exception (msg); e2.initCause(e); e.printStackTrace();}");
               writerOutput.println("    public void trace(String msg) { System.out.println(msg); }");
               writerOutput.println("    public void trace(String msg, Throwable e) { Exception e2 = new Exception (msg); e2.initCause(e); e.printStackTrace();}");
               writerOutput.println("    public void warn(String msg) { System.out.println(msg); }");
               writerOutput.println("    public void warn(String msg, Throwable e) { Exception e2 = new Exception (msg); e2.initCause(e); e.printStackTrace();}");
               writerOutput.println("    public void error(String msg) { System.out.println(msg); }");
               writerOutput.println("    public void error(String msg, Throwable e) { Exception e2 = new Exception (msg); e2.initCause(e); e.printStackTrace();}");

               writerOutput.println("}");

               writerOutput.close();
            }
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
         processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
         return false;
      }

      return true;
   }

   private void generateMessage(Bundle bundleAnnotation, PrintWriter writerOutput, ExecutableElement executableMember, Message messageAnnotation)
   {
      // This is really a debug output
      writerOutput.println("   // " + messageAnnotation.toString());

      writerOutput.write("   public " + executableMember.getReturnType() + " " + executableMember.getSimpleName() + "(");


      Iterator<? extends VariableElement> parameters = executableMember.getParameters().iterator();


      boolean hasParameters = false;

      // the one that will be used on the call
      StringBuffer callList = new StringBuffer();
      while (parameters.hasNext())
      {
         hasParameters = true;
         VariableElement parameter = parameters.next();
         writerOutput.write(parameter.asType() + " " + parameter.getSimpleName());
         callList.append(parameter.getSimpleName());
         if (parameters.hasNext())
         {
            writerOutput.write(", ");
            callList.append(",");
         }
      }


      // the real implementation
      writerOutput.println(")");
      writerOutput.println("   {");

      String formattingString = bundleAnnotation.projectCode() + messageAnnotation.id() + " " + messageAnnotation.value();
      if (!hasParameters)
      {
         writerOutput.println("      String returnString = \"" + formattingString + "\";");
      }
      else
      {
         writerOutput.println("      String returnString = java.text.MessageFormat.format(\"" + formattingString + "\"," + callList + ");");
      }


      if (executableMember.getReturnType().toString().equals(String.class.getName()))
      {
         writerOutput.println("      return returnString;");
      }
      else
      {
         writerOutput.println("      return new " + executableMember.getReturnType().toString() + "(returnString);");
      }

      writerOutput.println("   }");
      writerOutput.println();
   }

   private void generateLogger(Bundle bundleAnnotation, PrintWriter writerOutput, ExecutableElement executableMember, LogMessage messageAnnotation)
   {
      // This is really a debug output
      writerOutput.println("   // " + messageAnnotation.toString());

      writerOutput.write("   public void " + executableMember.getSimpleName() + "(");


      Iterator<? extends VariableElement> parameters = executableMember.getParameters().iterator();


      boolean hasParameters = false;

      // the one that will be used on the call
      StringBuffer callList = new StringBuffer();
      while (parameters.hasNext())
      {
         hasParameters = true;
         VariableElement parameter = parameters.next();
         writerOutput.write(parameter.asType() + " " + parameter.getSimpleName());
         callList.append(parameter.getSimpleName());
         if (parameters.hasNext())
         {
            writerOutput.write(", ");
            callList.append(",");
         }
      }


      // the real implementation
      writerOutput.println(")");
      writerOutput.println("   {");


      // TODO: Better Logging here
      String formattingString = bundleAnnotation.projectCode() + messageAnnotation.id() + " " + messageAnnotation.value();
      if (!hasParameters)
      {
         writerOutput.println("      String messageString = \"" + formattingString + "\";");
      }
      else
      {
         writerOutput.println("      String messageString = java.text.MessageFormat.format(\"" + formattingString + "\"," + callList + ");");
      }

      writerOutput.println("      System.out.println(messageString);");

      writerOutput.println("   }");
      writerOutput.println();
   }
}
