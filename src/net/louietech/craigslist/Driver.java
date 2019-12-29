package net.louietech.craigslist;

public class Driver {
	public static void main(String[] args) {
		FetchAndParse fetchAndParse = new FetchAndParse();
		fetchAndParse.SetSendTextMessages(true);
		fetchAndParse.Start();
	}
}
