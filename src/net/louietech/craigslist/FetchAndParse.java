package net.louietech.craigslist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchAndParse {
	
	  /////////////////////////////////////
	 //			Class Variables			//
	/////////////////////////////////////
	
	private final String smtpServer = "mai.server.com";//customize
	private final String emailRecipients = "";//customize
	private final String emailSenderAddress = "";//customize
	private final String yourUsername = "";//customize
	private final String yourPassword = "";//customize
	private final String URL = "";//customize
	
	private ArrayList<String> arrayOfHtml = new ArrayList<String>();
	private ArrayList<String> arrayOfLinks = new ArrayList<String>();
	private ArrayList<String> arrayOfMasterLinks = new ArrayList<String>();
	private String nameOfMasterFile = "";
	
	  /////////////////////////////////////
	 //			Constructor(s)			//
	/////////////////////////////////////
	
	public FetchAndParse () {}
	
	  /////////////////////////////////
	 //			Main Function		//
	/////////////////////////////////
	
	public void Start () {
		
		//Determine the OS and set path to master file
		DetermineOS();		
		ReadLinksFromMasterFile();
		
		//fetch the webpage and parse the page for links
		Document doc = null;
		try {
			doc = Jsoup.connect(URL).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Elements listOfAllLinks = doc.select("a[href*=/cto/]");
		
		//Initialize counter and string with a little html 
		int i=0;
		String StringToBeSent = "<ol>";

		//for each link  in array, go over array and determine if the link has already been sent,
		//if the link has been sent skip it, if not then take link and text to build html for mailing later
		//
		//counter is used to do every other link as the first link's text is the price and the second is 
		//the description of the item
		for (Element link : listOfAllLinks) {
			String tempLink = link.attr("href");
			String tempText = link.text();
			
			if(!arrayOfMasterLinks.contains(tempLink)) {		
				if(!arrayOfLinks.contains(tempLink)) {
					arrayOfLinks.add(tempLink);
				}
	            if(i==0) {
	            	StringToBeSent = StringToBeSent + "<li>" + tempText + " --- " + "<a href=" + tempLink + ">";
	            	i++;
	            }
	            else {
	            	StringToBeSent = StringToBeSent + tempText + "</a></li>";
	            	arrayOfHtml.add(StringToBeSent);
	            	StringToBeSent = "";
	            	i=0;
	            }
			}
		}
		
		//if any new links were found send an email with the links, if not print no new cars to report
		if(arrayOfLinks.size() > 0) {
			SendEmailToUsers();
			WriteLinksToMasterFile();
		}
		else {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " - No cars to report.");
		}
	}
	
	//
	//Function to determine os type and set path to master text file
	//
	private void DetermineOS() {
		String osType = System.getProperty("os.name");
		if(osType.contains("Windows")) {
			nameOfMasterFile = "E:\\Louie\\Documents\\Java Dev\\workspace\\CraigslistScraper\\master.txt";
		}
		else if(osType.contains("Linux")) {
			nameOfMasterFile = "/home/louie/craigslist/master.txt";
		}
		else {
			System.out.println("Could not determine OS type");
		}
	}

	//
	//Function to check if master file exists yet and create it if not 
	//then to read all links from the master file into a variable for checking later
	//
	private void ReadLinksFromMasterFile () {
		File f = new File(nameOfMasterFile);
		if(!f.isFile()) {
			PrintWriter writer;
			try {
				writer = new PrintWriter(nameOfMasterFile, "UTF-8");
				writer.println("");
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(nameOfMasterFile))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	arrayOfMasterLinks.add(line);
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	//
	//Function to write all links in variable to file
	//
	private void WriteLinksToMasterFile () {
		Path file = Paths.get(nameOfMasterFile);
		try {
			Files.write(file, arrayOfLinks, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//
	//Function to send html email to users of new links discovered
	//
	private void SendEmailToUsers () {
		// Get system properties
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", smtpServer);
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
  
    	try { 
	        Session session = Session.getDefaultInstance(properties,
	        	new javax.mail.Authenticator() {
	        		protected PasswordAuthentication getPasswordAuthentication() {
	        			return new PasswordAuthentication(yourUsername, yourPassword);
	        		}
	        	}); 
	  
		    MimeMessage message = new MimeMessage(session);
		    message.setFrom(new InternetAddress(emailSenderAddress));
		    message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailRecipients));
		    message.setSubject("Craigslist Cars!");
		    
		    String stringOfHtml = "";
		    for (String s : arrayOfHtml) {
		    	stringOfHtml = stringOfHtml + s;
		    }
		    
		    message.setContent(stringOfHtml,"text/html"); 
		  
		    Transport.send(message);
		    
		    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " - Email Sent.");
		} 
    	catch (MessagingException e) { 
		    e.printStackTrace(); 
		}
	}
}
