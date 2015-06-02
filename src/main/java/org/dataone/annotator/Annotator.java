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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
		Option nThreads = 
				OptionBuilder.withArgName("nThreads")
                .hasArg()
                .withDescription("use given number of threads")
                .create("nThreads");
		Option create = new Option("create", "generate the annotations");
		Option remove = new Option("remove", "remove the annotations");
		Option removeAll = new Option("removeAll", "remove ALL annotations");
		Options options = new Options();
		
		options.addOption(create);
		options.addOption(remove);
		options.addOption(removeAll);
		options.addOption(pidFile);
		options.addOption(nThreads);

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
		
		final Session session = setUpSession();
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

        final CommandLine cmdRef = cmd;
        
        // create or remove annotations
        int nThreads = 1;
        if (cmd.hasOption("nThreads")) {
        	// how many threads
        	nThreads = Integer.valueOf(cmd.getOptionValue("nThreads"));
        }
        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        
        int size = identifiers.size();
        int segment = size / nThreads;
        int fromIndex = 0;
        int toIndex = 0;
		
        while (toIndex < identifiers.size()) {
        
	        fromIndex = toIndex;
        	toIndex = toIndex + segment;
	        toIndex = Math.min(toIndex, size);
	        
	        final List<String> subList = identifiers.subList(fromIndex, toIndex);
	        
	        log.debug("processing sublist from: " + fromIndex + " to: " + toIndex);
	        
	        exec.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
				    	AnnotationUploader uploader = new AnnotationUploader(session);
				        if (cmdRef.hasOption("create")) {
							uploader.createAnnotationsFor(subList);
				        } else if (cmdRef.hasOption("remove")) {
				        	uploader.removeAnnotationsFor(subList);
				        } else if (cmdRef.hasOption("removeAll")) {
				        	uploader.removeAll();
				        }
			        } catch (Exception e) {
			        	e.printStackTrace();
			        }
					
				}
			});
	        
	        
        }
        
    	exec.shutdown();

        try {
			exec.awaitTermination(3, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}
		
	}

}
