/* vim: set ts=4 sw=4 et: */

package org.gitorious.scrapfilbleu.android;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.util.Log;

import android.os.Bundle;
import android.os.AsyncTask;

import android.view.View;

import android.widget.Toast;
import android.widget.Button;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ExpandableListView;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.ExpandableListView.OnChildClickListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BusToursActivity extends Activity
{
	private static Context context;
    private DatePicker date;
    private TimePicker time;
    private AutoCompleteTextView txtCityDeparture;
    private AutoCompleteTextView txtStopDeparture;
    private Spinner sens;
    private AutoCompleteTextView txtCityArrival;
    private AutoCompleteTextView txtStopArrival;
    private Spinner listCriteria;
    private Button btnGetJourney;

    private Dialog journeyList;
    private Dialog journeyDetails;

    private String[] journeyCriteriaValues;
    private String[] sensValues;

    private ArrayList<Journey> journeys;
    private int journeyDetailsProcessing;

    private URLs urls;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.context = this;
        this.date               = (DatePicker)findViewById(R.id.date);
        this.time               = (TimePicker)findViewById(R.id.time);
        this.txtCityDeparture   = (AutoCompleteTextView)findViewById(R.id.txtCityDeparture);
        this.txtStopDeparture   = (AutoCompleteTextView)findViewById(R.id.txtStopDeparture);
        this.sens               = (Spinner)findViewById(R.id.Sens);
        this.txtCityArrival     = (AutoCompleteTextView)findViewById(R.id.txtCityArrival);
        this.txtStopArrival     = (AutoCompleteTextView)findViewById(R.id.txtStopArrival);
        this.listCriteria       = (Spinner)findViewById(R.id.listCriteria);
        this.btnGetJourney      = (Button)findViewById(R.id.btnGetJourney);

        this.journeyCriteriaValues  = getResources().getStringArray(R.array.journeyCriteriaValues);
        this.sensValues             = getResources().getStringArray(R.array.sensValues);

        this.fill();
        this.bindWidgets();
    }
    
    public void fill()
    {
        // fill journey criteria
        ArrayAdapter<CharSequence> criteriaAdapter = ArrayAdapter.createFromResource(this, R.array.journeyCriteria, android.R.layout.simple_spinner_item);
        criteriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.listCriteria.setAdapter(criteriaAdapter);

        // fill sens
        ArrayAdapter<CharSequence> sensAdapter = ArrayAdapter.createFromResource(this, R.array.sens, android.R.layout.simple_spinner_item);
        sensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.sens.setAdapter(sensAdapter);

        BusCities cities = new BusCities();
        BusStops stops = new BusStops();

        // fill city autocomplete
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, cities.getCities());
        this.txtCityDeparture.setAdapter(cityAdapter);
        this.txtCityArrival.setAdapter(cityAdapter);

        // fill stop autocomplete
        ArrayAdapter<String> stopAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, stops.getStops());
        this.txtStopDeparture.setAdapter(stopAdapter);
        this.txtStopArrival.setAdapter(stopAdapter);
    }

    public void bindWidgets()
    {
        this.txtCityDeparture.setThreshold(1);
        this.txtCityArrival.setThreshold(1);
        this.txtStopDeparture.setThreshold(1);
        this.txtStopArrival.setThreshold(1);
        this.time.setIs24HourView(true);
        this.btnGetJourney.setOnClickListener(new View.OnClickListener() { public void onClick(View arg0) { onClick_btnGetJourney(); } });
    }

    public int getJourneyCriteriaValue()
    {
        return Integer.parseInt(this.journeyCriteriaValues[this.listCriteria.getSelectedItemPosition()]);
    }

    public int getSensValue()
    {
        return Integer.parseInt(this.sensValues[this.sens.getSelectedItemPosition()]);
    }

    public void onClick_btnGetJourney()
    {
        BusJourney j = new BusJourney();
        j.setCityDep(this.txtCityDeparture.getEditableText().toString());
        j.setCityArr(this.txtCityArrival.getEditableText().toString());
        j.setStopDep(this.txtStopDeparture.getEditableText().toString());
        j.setStopArr(this.txtStopArrival.getEditableText().toString());
        j.setDate(new String()
            + String.valueOf(this.date.getDayOfMonth())
            + "/"
            + String.valueOf(this.date.getMonth())
            + "/"
            + String.valueOf(this.date.getYear())
        );
        j.setHour(String.valueOf(this.time.getCurrentHour()));
        j.setMinute(String.valueOf(this.time.getCurrentMinute()));
        j.setSens(String.valueOf(this.getSensValue()));
        j.setCriteria(String.valueOf(this.getJourneyCriteriaValue()));
        new ProcessScrapping().execute(j);
    }

    public static void messageBox(String text) {
		Toast.makeText(context,text, Toast.LENGTH_SHORT).show();
	}

	public AlertDialog alertBox(String title, String text) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this.context);
		dialog.setTitle(title);
		dialog.setMessage(text);
		dialog.setCancelable(false);
		dialog.setPositiveButton(
			getString(R.string.okay),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			}
		);
		return dialog.create();
	}

	public void alertInfoBox(String title, String text) {
		AlertDialog d = alertBox("[" + getString(R.string.msgInfoTitle) + "]: " + title, text);
		d.show();
	}

	public void alertErrorBox(String title, String text) {
		AlertDialog d = alertBox("[" + getString(R.string.msgErrorTitle) + "]: " + title, text);
		d.show();
	}

    public void setJourneys(ArrayList<Journey> js) {
        this.journeys = js;
    }

    public ArrayList<Journey> getJourneys() {
        return this.journeys;
    }

    public void setJourneyDetailsProcessing(int v) {
        this.journeyDetailsProcessing = v;
    }

    public void onAsyncTaskScrapJourneyDetailsComplete() {
        Log.e("BusTours", "Got details for " + this.journeyDetailsProcessing);
        Journey targetJourney = this.journeys.get(this.journeyDetailsProcessing);
        JourneyDetails details = targetJourney.getJourneyDetails();

        if (details == null) {
            Log.e("BusTours", "No details available for " + this.journeyDetailsProcessing);
            this.alertInfoBox(getString(R.string.noDetails), getString(R.string.noDetailsTxt));
            return;
        }

        ExpandableListView list;
        journeyDetails = new Dialog(this);
        journeyDetails.setContentView(R.layout.details);
        journeyDetails.setTitle(getString(R.string.journey_details));

        String[] fromGroup = new String[] { "head" };
        int[] toGroup = new int[] { android.R.id.text1 };
        String[] fromChild = new String[] { "head", "more" };
        int[] toChild = new int[] { android.R.id.text1, android.R.id.text2 };

        List<HashMap<String, String>> jList = new ArrayList<HashMap<String, String>>();
        List<List<HashMap<String, String>>> jListChild = new ArrayList<List<HashMap<String, String>>>();

        Iterator<JourneyDetails.JourneyPart> jit = details.getParts().iterator();
        while (jit.hasNext()) {
            JourneyDetails.JourneyPart jp = (JourneyDetails.JourneyPart)jit.next();
            JourneyDetails.Indication indic = jp.getIndic();
            HashMap<String, String> map = new HashMap<String, String>();
            List<HashMap<String, String>> children = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> curChildMap = new HashMap<String, String>();

            if (jp.getType().equals("indication")) {
                map.put("head", jp.getTime() + ": " + getString(R.string.stopIndic) + " '" + indic.getStop() + "'");

                if (indic.getType().equals("mount")) {
                    curChildMap.put("head", getString(R.string.detailLine) + " " + indic.getLine());
                    curChildMap.put("more", getString(R.string.detailDirection) + " " + indic.getDirection());
                }

                if (indic.getType().equals("umount")) {
                    curChildMap.put("head", getString(R.string.detailUmount));
                    curChildMap.put("more", "");
                }
            }

            if (jp.getType().equals("connection")) {
                map.put("head", getString(R.string.connectionInfo));
                curChildMap.put("head", getString(R.string.connectionDuration) + " " + jp.getDuration());
                curChildMap.put("more", "");
            }

            children.add(curChildMap);
            jListChild.add(children);
            jList.add(map);
        }

        list = (ExpandableListView)journeyDetails.findViewById(R.id.listJourneyDetails);
        ExpandableListAdapter journeyDetailsAdapter = new SimpleExpandableListAdapter(
            this,
            jList,
            android.R.layout.simple_expandable_list_item_2,
            fromGroup, toGroup,
            jListChild,
            android.R.layout.simple_expandable_list_item_2,
            fromChild, toChild
        );

        list.setAdapter(journeyDetailsAdapter);

        journeyDetails.show();
    }

    public void onAsyncTaskScrapJourneyListComplete() {
        if (this.journeys == null) {
            Log.e("BusTours", "No journey to display");
            return;
        }

        ExpandableListView list;
        journeyList = new Dialog(this);
        journeyList.setContentView(R.layout.journey);
        journeyList.setTitle(getString(R.string.journey_list));

        String[] fromGroup = new String[] { "head" };
        int[] toGroup = new int[] { android.R.id.text1 };
        String[] fromChild = new String[] { "head", "more" };
        int[] toChild = new int[] { android.R.id.text1, android.R.id.text2 };

        List<HashMap<String, String>> jList = new ArrayList<HashMap<String, String>>();
        List<List<HashMap<String, String>>> jListChild = new ArrayList<List<HashMap<String, String>>>();

        Iterator<Journey> jit = this.journeys.iterator();
        while (jit.hasNext()) {
            Journey j = (Journey)jit.next();

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("head", j.getDepartureTime() + " - " + j.getArrivalTime() + " (" + j.getDuration() + ")");

            List<HashMap<String, String>> children = new ArrayList<HashMap<String, String>>();

            HashMap<String, String> curChildMap = new HashMap<String, String>();
            curChildMap.put("head", "Duration: " + j.getDuration());
            curChildMap.put("more", "Connections: " + j.getConnections());
            children.add(curChildMap);

            jListChild.add(children);

            jList.add(map);
        }

        list = (ExpandableListView)journeyList.findViewById(R.id.listJourneys);
        list.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                try {
                    Journey journey = getJourneys().get(groupPosition);
                    setJourneyDetailsProcessing(groupPosition);
                    Log.e("BusTours", "groupPosition:" + String.valueOf(groupPosition));
                    Log.e("BusTours", "journey:" + journey);
                    if (journey.getJourneyDetails() == null) {
                        Log.e("BusTours", "No details for this journey, starting scrapping ...");
                        new ProcessScrapping().execute(journey);
                    } else {
                        Log.e("BusTours", "Details already available.");
                        onAsyncTaskScrapJourneyDetailsComplete();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        ExpandableListAdapter journeyAdapter = new SimpleExpandableListAdapter(
            this,
            jList,
            android.R.layout.simple_expandable_list_item_2,
            fromGroup, toGroup,
            jListChild,
            android.R.layout.simple_expandable_list_item_2,
            fromChild, toChild
        );

        list.setAdapter(journeyAdapter);

        journeyList.show();
    }

    public class ProcessScrapping extends AsyncTask<Object, Integer, Boolean> {
        private Exception exc;
        private String processing;

        // Showing Async progress
        private Dialog dialog;
        private TextView statusProgressHttp;
        private ProgressBar progressHttp;

        public void progress(Integer ... progress) {
            this.publishProgress(progress);
        }

        protected void onPreExecute() {
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.progress);
            dialog.setTitle(getString(R.string.scrapping));

            statusProgressHttp = (TextView) dialog.findViewById(R.id.statusProgressHttp);
            progressHttp = (ProgressBar) dialog.findViewById(R.id.progressHttp);
            progressHttp.setMax(100);

            dialog.show();
        }

        protected Boolean doInBackground(Object ... journey) {
            String className = journey[0].getClass().getSimpleName();
            publishProgress(0, R.string.startHttpScrapping);

            Log.e("BusTours", "Processing " + className);
            this.processing = className;

            try {
                publishProgress(10, R.string.jsoupConnect);
                if (className.equals("BusJourney")) {
                    BusJourney j = (BusJourney)journey[0];
                    setJourneys(j.getBusJourneys(this));
                }
                if (className.equals("Journey")) {
                    Journey j = (Journey)journey[0];
                    j.getDetails(this);
                }
                publishProgress(100, R.string.jsoupDocReady);
                return true;
            } catch (Exception e) {
                this.exc = e;
                e.printStackTrace();
                return false;
            }
        }

        protected void onProgressUpdate(Integer ... progress) {
            progressHttp.setProgress(progress[0]);
            statusProgressHttp.setText(getString(progress[1]));
        }

        protected void onPostExecute(Boolean result) {
            dialog.dismiss();

            if (!result) {
                String excName = this.exc.getClass().getSimpleName();
                String msg = "";

                Log.e("BusTours", "Got exception: " + excName);

                if (excName.equals("SocketTimeoutException")) {
                    Log.e("BusTours", "Got SocketTimeoutException");
                    msg = getString(R.string.networkError);
                }

                if (excName.equals("IOException")) {
                    Log.e("BusTours", "Got IOException");
                }

                if (excName.equals("ScrappingException")) {
                    Log.e("BusTours", "Got ScrappingException");
                    ScrappingException e = (ScrappingException)(this.exc);
                    msg = getString(R.string.scrappError) + ": " + e.getError();
                }

                if (msg.length() != 0) {
                    Log.e("BusTours", "msg=" + msg);
                    alertErrorBox(excName, msg);
                }
            }

            if (this.processing.equals("BusJourney")) {
                onAsyncTaskScrapJourneyListComplete();
            }

            if (this.processing.equals("Journey")) {
                onAsyncTaskScrapJourneyDetailsComplete();
            }
        }
    }
}
