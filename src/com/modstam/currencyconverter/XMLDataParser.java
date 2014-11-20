package com.modstam.currencyconverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.os.Environment;

public class XMLDataParser {

	private static XmlPullParser parser;
	private static String exchangeURL = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
	public static String file = "savedRates";
	private static long lastSave = 0;

	static String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

	public static boolean parseXMLData(Context context, HashMap<String, Float> conversions, ArrayList<String> currencies) {
		HttpURLConnection http = null;
		try {

			parser = XmlPullParserFactory.newInstance().newPullParser();
			// Open a connection to ECB
			URL url = new URL(exchangeURL);
			http = (HttpURLConnection) url.openConnection();
			// Send a stream to parse which will go through it
			parse(http.getInputStream(), conversions, currencies);
			// If we couldnt get a connection, disconnect
			if (http != null)
				http.disconnect();

		} catch (Exception e) {
			System.err.println("Caught an exception while trying to parse XML data: \n" + e);
			// We didn't get a connection, lets see if we can get data from
			// files
			return readFiles(context, Long.MAX_VALUE, conversions, currencies);
		}
		// Sort and save to file
		Collections.sort(currencies);
		saveFiles(context, conversions, currencies);
		return true;

	}

	private static void parse(InputStream xmlStream, HashMap<String, Float> conversions, ArrayList<String> currencies)
			throws Exception {

		parser.setInput(xmlStream, null); // reset parser
		int parseEvent = parser.getEventType(); // get first tag type
		System.err.println("Started parsing input stream");
		conversions.clear(); // clear lists
		currencies.clear();

		// Add euro manually, (it's always 1)
		conversions.put("EUR", 1.0f);
		currencies.add("EUR");

		while (parseEvent != XmlPullParser.END_DOCUMENT) {
			switch (parseEvent) {
			// adapted the switch from one of the examples
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if (tagName.equalsIgnoreCase("cube")) {

					if (parser.getAttributeCount() == 1) {
						// Do nothing with the date at the moment

					} else if (parser.getAttributeCount() == 2) {
						// put names of currencies into "currencies"
						// put (names,rate) into the hashmap "conversions"
						String currency = parser.getAttributeValue(0);
						Float rate = Float.valueOf(parser.getAttributeValue(1));
						System.err.println("Cube currency=" + currency + "   rate=" + rate);
						conversions.put(currency, rate);
						currencies.add(currency);
					}
				}
				break;
			default:
				//
			}

			parseEvent = parser.next();
		}
	}

	public static boolean readFiles(Context context, long offset, HashMap<String, Float> conversions,
			ArrayList<String> currencies) {
		try {
			// Try to open a stream to our internal file
			FileInputStream inputStream = context.openFileInput(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			// Iterate through the file
			while ((line = reader.readLine()) != null) {
				String[] tmp = line.split(":");
				if (tmp[0].equals("time")) {
					currencies.clear();
					conversions.clear();
					lastSave = Long.parseLong(tmp[1]);
					// We use offset here to determine if we need to update
					// If we need to update, we dont want to read the file
					// and will return false
					if (lastSave < offset) {
						return false;
					}

				} else {
					// add our currencies and conversions to the lists
					conversions.put(tmp[0], Float.parseFloat(tmp[1]));
					currencies.add(tmp[0]);
					System.out.println("Loaded " + tmp[0] + ":" + tmp[1] + " from file");
				}
			}
			// Close stream and sort
			reader.close();
			System.err.println("Successfully read files");
			Collections.sort(currencies);
			return true;
		} catch (IOException e) {
			System.err.println("Couldn't read files");
			e.printStackTrace();
			return false;
		}

	}

	public static boolean saveFiles(Context context, HashMap<String, Float> conversions, ArrayList<String> currencies) {

		try {
			// Start writing to file
			FileOutputStream outputStream = context.openFileOutput(file, Context.MODE_PRIVATE);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
			Iterator it = conversions.entrySet().iterator();
			lastSave = System.currentTimeMillis();
			writer.println("time:" + lastSave);
			// iterate through the hashmap's entrySet
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				System.out.println("Writing '" + pairs.getKey() + ":" + pairs.getValue() + "' to file");
				// write to file and flush
				writer.println("" + pairs.getKey() + ":" + pairs.getValue());
				writer.flush();
			}
			writer.close();
			System.err.println("Successfully saved currencies");
		} catch (IOException e) {
			System.err.println("Couldn't save currencies");
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
