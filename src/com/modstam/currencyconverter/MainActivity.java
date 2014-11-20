package com.modstam.currencyconverter;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	private HashMap<String, Float> conversions; // for conversion rates
	private ArrayList<String> currencies; // for currency names
	private Context context = this;
	private long updateTime = 86400000; // 24 hours in milliseconds

	// private long updateTime = 0;

	@Override
	public void onStart() {
		super.onStart();
		// start by setting the loadingpanel to visible and disable the currency
		// layout
		findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
		findViewById(R.id.currency).setVisibility(View.GONE);
		System.err.println("Starting up!");
		// the async task will check if we need to update
		XMLTask task = new XMLTask(this);
		task.execute();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Initialize fields
		this.conversions = new HashMap<String, Float>();
		this.currencies = new ArrayList<String>();
		// Get a reference to the editText-field in the UI and add a listener
		EditText editText = (EditText) findViewById(R.id.editText1);
		editText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			/* Handle input in the textfield */
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				// If the user pressed "Done" , we want to update the UI
				// accordingly
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					// Get references to spinners and textfields
					EditText editText = (EditText) findViewById(R.id.editText1);
					Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
					Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
					// Get the current selection of the spinners
					String to = spinner1.getSelectedItem().toString();
					String from = spinner2.getSelectedItem().toString();
					try {
						// get the value that was input into the textfield and
						// calculate the conversion, then display it in the ui
						float amount = Float.parseFloat(editText.getText().toString());
						TextView textViewToChange = (TextView) findViewById(R.id.equals);
						textViewToChange.setText("= " + calculate(from, to, amount));
						handled = true;
					} catch (Exception e) {
						showToast("Please enter a number");
						System.err.println(e);
					}
				}
				return handled;
			}

		});

		final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			/*
			 * Handle buttonpresses This method is for removing previous saved
			 * files and retrieving new data from the internet
			 */
			@Override
			public void onClick(View v) {

				// Get new data by calling for an async-xml parsing task
				// And forcing it to update
				updateTime = 0;
				findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
				findViewById(R.id.currency).setVisibility(View.GONE);
				XMLTask task = new XMLTask(context);
				task.execute();
			}
		});

	}

	private class XMLTask extends AsyncTask<Long, Void, String> {
		private Context context;

		protected XMLTask(Context context) {
			this.context = context;
		}

		/**
		 * doInBackground - the heavy task (calculate primes). This method will
		 * be executed on a separate thread.
		 */
		@Override
		protected String doInBackground(Long... params) {
			// Readfiles returns false if update is needed
			if (!XMLDataParser.readFiles(context, System.currentTimeMillis() - updateTime, conversions, currencies)) {
				// Get new data from the internet
				if (!XMLDataParser.parseXMLData(context, conversions, currencies)) {
					// This will only happen if we have no connection and no
					// stored files
					return "Couldn't retrieve data";
				}
				return "Retrieved currency data";
			} else {
				return "Retrieved from files";
			}
		}

		/**
		 * onPostExecute - update the UI. This method will be executed on the
		 * UI-thread.
		 */
		@Override
		protected void onPostExecute(String output) {

			// Make sure that the right thread is updating the UI (only the
			// creator-thread can update the ui)
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Create an adapter from our currencies-list and structure
					// it for dropdown format
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
							android.R.layout.simple_spinner_dropdown_item, currencies);
					// References to spinners
					Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
					Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
					// Update spinners with the data from the adapter
					spinner1.setAdapter(adapter);
					spinner2.setAdapter(adapter);
					// Update visibilities of our layouts
					findViewById(R.id.loadingPanel).setVisibility(View.GONE);
					findViewById(R.id.currency).setVisibility(View.VISIBLE);
					updateTime = 86400000; // reset time in case it was 0
				}
			});

			showToast(output);
		}

	}

	private void showToast(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	public String calculate(String from, String to, float amount) {
		// Simply calculates the conversion between two currencies
		float rateFrom = conversions.get(from);
		float rateTo = conversions.get(to);
		return amount * (rateTo / rateFrom) + " " + to;
	}

}
