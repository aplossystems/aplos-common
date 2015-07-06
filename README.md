# aplos-common
Common functionality required for all Aplos Architecture projects.

Welcome to the Aplos Archictecture 
----------------------------------------
The Aplos Architecture has been created by <a href="https://www.aplossystems.co.uk">Aplos Systems</a> since it was established in 2008.  It is the codebase that has been built to help it create affordable, professional and feature rich <a href="https://www.aplossystems.co.uk/bespoke-software.aplos">bespoke software</a> for it's clients.  It has now been released as an open source project to try and give back to the online community that has been so helpful in provided many of the open source projects that it is based on.

Configuration
-----------------------------------------
The system requires:

<ul>
<li>An installation of Java JDK 1.7 (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)</li>
<li>Eclipse http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/marsr with maven installed</li>
<li>Install subsclipse into Eclipse with the update URL (http://subclipse.tigris.org/update_1.10.x)</li>
<li>Install AspectJ tools into Ecipse with the update URL (http://download.eclipse.org/tools/ajdt/44/dev/update)
<li>Tomcat 7.0 (https://tomcat.apache.org/download-70.cgi)</li>
<li>MySql (At writing we are using 5.6.25 https://dev.mysql.com/downloads/mysql/)</li>
</ul>

Once you have these installed and ready to go then you can download all of these aplos systems modules:
<ul>
<li>aplos-archetype</li>
<li>aplos-root</li>
<li>aplos-parentroot</li>
<li>aplos-common</li>
<li>aplos-core</li>
<li>aplos-cms (Optional)</li>
</ul>

Once you have checked these out then you'll need to convert them to maven projects and run the maven install on them.  

