package org.dataone.annotator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.AnnotationUploader;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;

public class Annotator {

	private static Log log = LogFactory.getLog(Annotator.class);

	private static Options setUpOptions() {
		Option pidFile = 
				OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("use given file for pid listing")
                .create("pidfile");
		Option create = new Option("create", "generate the annotations");
		Option remove = new Option("remove", "remove the annotations");
		Options options = new Options();
		
		options.addOption(create);
		options.addOption(remove);
		options.addOption(pidFile);

		return options;

		
	}
	
	private static Session setUpSession() {
				
		X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
		if (certificate == null) {
			return null;
		}
			
		PrivateKey key = CertificateManager.getInstance().loadKey();
		String subjectDN = CertificateManager.getInstance().getSubjectDN(certificate);
		CertificateManager.getInstance().registerCertificate(subjectDN, certificate, key );
		Session session = new Session();
		Subject subject = new Subject();
		subject.setValue(subjectDN);
		session.setSubject(subject);
		
		return session;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");
		
		Session session = setUpSession();
		Options options = setUpOptions();
		
        // parse the command line arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
        try {
        	cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
	    
        // gather the pids
		List<String> identifiers = new ArrayList<String>();
        try {
	        if (cmd.hasOption("pidfile")) {
	        	// read the pids from this file
	        	String pidFile = cmd.getOptionValue("pidfile");
	        	URL url = new URL(pidFile);
	        	InputStream inputStream = url.openStream();
	    		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
	    		String line;
	    		while ((line = reader.readLine()) != null) {
	    			identifiers.add(line);
	    		}
	        	
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        }

        // create or remove annotations
        try {
	    	AnnotationUploader uploader = new AnnotationUploader(session);
	        if (cmd.hasOption("create")) {
				uploader.createAnnotationsFor(identifiers);
	        } else if (cmd.hasOption("remove")) {
	        	uploader.removeAnnotationsFor(identifiers);
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
	}

}
