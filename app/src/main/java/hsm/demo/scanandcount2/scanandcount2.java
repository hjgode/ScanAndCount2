package hsm.demo.scanandcount2;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.*;
import com.honeywell.aidc.BarcodeReader.TriggerListener;

import java.util.Date;
import java.util.List;

public class scanandcount2 extends AppCompatActivity implements BarcodeReader.BarcodeListener, SharedPreferences.OnSharedPreferenceChangeListener{
    static String TAG = "ScanAndCount2: ";
    EditText txtData;
    EditText txtQuantity;
    EditText txtLagerort;

    EditText txtCurrent; //hold current focussed EditText, enables to scan to txtData, (txtQuantity), txtLagerort -> txtData

    enum state {        //create a state machine
        data,
        quantity,
        lager,
    }

    state currentState = state.data;

    Button btnCancel;
    Button btnSave;
//    CheckBox chkLagerort;
//    CheckBox chkScanQuantity;
//    CheckBox chkScanLager;

    LinearLayout panelLagerort;

//    Button btnList;
//    Button btnClear;
//    Button btnExport;

    Button btnKeybd;
    boolean bIsKeydbVisible = false;

    //persist and check if Lagerort will be shown or not
    boolean mUseLagerort = false;
    boolean mScanQuantity=false;
    boolean mScanLager=false;

//    private static final String USE_LAGERORT = "USE_LAGERORT";
//    private static final String USE_SCANLAGER = "USE_SCANLAGER";
//    private static final String USE_SCANQUANTITY = "USE_SCANQUANTITY";

    DataReaderDBHelper mDbHelper;

    private BarcodeReader barcodeReader;
    private AidcManager manager;

    SharedPreferences prefs;

    /***
     * react on changes in preferences activity
     * @param sharedPreferences
     * @param key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        mUseLagerort= prefs.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_LAGERORT_ENABLED, false);
        mScanLager=prefs.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_SCAN_LAGERORT_ENABLED, false);
        mScanQuantity=prefs.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_SCAN_MENGE_ENABLED,false);
        showHideLagerort();//...
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanandcount2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        //load defaults only if never called before
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs=PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadPrefs();

        Log.e(TAG, "onCreate: mUseLagerort=" + mUseLagerort);

        mDbHelper = new DataReaderDBHelper(getApplicationContext());

        txtData = (EditText) findViewById(R.id.txtData);
        //perform some action on focus change
        txtData.setOnFocusChangeListener(onFocusChange);

        txtData.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    txtQuantity.requestFocus();
                    return true;
                }
                return false;
            }
        });

        txtData.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (event == null) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // Capture soft enters in a singleLine EditText that is the last EditText.
                        txtQuantity.requestFocus();
                    } else if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        // Capture soft enters in other singleLine EditTexts
                        txtQuantity.requestFocus();
                    } else
                        return false;  // Let system handle all other null KeyEvents
                } else if (actionId == EditorInfo.IME_NULL) {
                    // Capture most soft enters in multi-line EditTexts and all hard enters.
                    // They supply a zero actionId and a valid KeyEvent rather than
                    // a non-zero actionId and a null event like the previous cases.
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        // We capture the event when key is first pressed.
                    } else
                        return true;   // We consume the event when the key is released.
                } else {
                    // We let the system handle it when the listener
                    // is triggered by something that wasn't an enter.
                    return false;
                }

                // Code from this point on will execute whenever the user
                // presses enter in an attached view, regardless of position,
                // keyboard, or singleLine status.

//                if (view==multiLineEditText)  multiLineEditText.setText("You pressed enter");
//                if (view==singleLineEditText)  singleLineEditText.setText("You pressed next");
//                if (view==lastSingleLineEditText)  lastSingleLineEditText.setText("You pressed done");
                return true;   // Consume the event
            }
        });


//        chkLagerort = (CheckBox) findViewById(R.id.chkUseLagerort);
//        chkLagerort.setChecked(mUseLagerort);
//        chkLagerort.setOnClickListener(myCheckBoxListener);
//
//        chkScanQuantity=(CheckBox)findViewById(R.id.chkScanQuantity);
//        chkScanQuantity.setChecked(mScanQuantity);
//        chkScanQuantity.setOnClickListener(myCheckBoxListener);
//
//        chkScanLager=(CheckBox)findViewById(R.id.chkScanLager);
//        chkScanLager.setChecked(mScanLager);
//        chkScanLager.setOnClickListener(myCheckBoxListener);

        showHideLagerort();

        txtLagerort = (EditText) findViewById(R.id.txtLagerort);
        txtLagerort.setOnFocusChangeListener(onFocusChange);
        txtLagerort.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        //detect ENTER or ESC key press on txtLagerort
        txtLagerort.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    if (mUseLagerort) {
                        saveData();
                        return true;
                    } else {
                        return false;
                    }
                }
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                    // Perform action on key press
                    //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    if (mUseLagerort) {
                        saveData();
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        });

        //detect DONE button press
        txtLagerort.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // do your stuff here
                    if (mUseLagerort) {
                        saveData();
                        return true;
                    } else
                        return false;
                }
                return false;
            }
        });

        txtQuantity = (EditText) findViewById(R.id.txtQuantity);
        //perform some action on focus change
        txtQuantity.setOnFocusChangeListener(onFocusChange);
        txtQuantity.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        //detect ENTER or ESC key press on txtQuantity
        txtQuantity.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    if (!mUseLagerort) {
                        saveData();
                        return true;
                    } else {
                        txtLagerort.requestFocus();
                        return false;
                    }
                }
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                    // Perform action on key press
                    //Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    if (!mUseLagerort) {
                        saveData();
                        return true;
                    } else {
                        txtLagerort.requestFocus();
                        return false;
                    }
                }
                return false;
            }
        });

        //detect DONE button press
        txtQuantity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // do your stuff here
                    if (!mUseLagerort) {
                        saveData();
                        return true;
                    } else {
                        txtLagerort.requestFocus();
                        return false;
                    }
                }
                return false;
            }
        });

        //link buttons
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnSave = (Button) findViewById(R.id.btnSave);

//        btnList = (Button) findViewById(R.id.btnList);
//        btnClear = (Button) findViewById(R.id.btnClear);
//        btnExport = (Button) findViewById(R.id.btnExport);

        btnKeybd = (Button) findViewById(R.id.btnKeybd);

        //variant 1
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOver();
            }
        });
        btnSave.setOnClickListener(myOnClickListener);
//        btnList.setOnClickListener(myOnClickListener);
//        btnClear.setOnClickListener(myOnClickListener);
//        btnExport.setOnClickListener(myOnClickListener);
        btnKeybd.setOnClickListener(myOnClickListener);

        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();

                try {
                    if (barcodeReader != null) {
                        Log.d(TAG, "barcodereader not claimed in OnCreate()");
                        barcodeReader.claim();
                    }
                    // apply settings
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                            BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);

//                    barcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
//                    barcodeReader.setProperty(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
//
//                    // set the trigger mode to automatic control
//                    barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
//                            BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
                } catch (UnsupportedPropertyException e) {
                    Toast.makeText(scanandcount2.this, "Failed to apply properties",
                            Toast.LENGTH_SHORT).show();

                } catch (ScannerUnavailableException e) {
                    Toast.makeText(scanandcount2.this, "Failed to claim scanner",
                            Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                }
                barcodeReader.addBarcodeListener(scanandcount2.this);
            }
        });
        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                Log.d("keyboard", "keyboard visible: " + isVisible);
                bIsKeydbVisible = isVisible;
            }
        });

        hideKeyboard();
        startOver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scanandcount2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(),ScanAndCountSettingsActivity.class);
            startActivity(i);
            return true;
        }else if(id==R.id.action_datafile_list){
            listData();
        }else if(id==R.id.action_datafile_clear){
            clearData();
        }else if(id==R.id.action_datafile_export){
            exportData();
        }

        return super.onOptionsItemSelected(item);
    }

    void showHideLagerort() {
        EditText mEdit = (EditText) findViewById(R.id.txtLagerort);
        mEdit.setEnabled(mUseLagerort);
        panelLagerort = (LinearLayout) findViewById(R.id.linearLayout2);
        try {
            if (mUseLagerort)
                panelLagerort.setVisibility(View.VISIBLE);
            else {
                panelLagerort.setVisibility(View.GONE);
            }
        } catch (Exception e) {
        }
//        chkScanQuantity.setChecked(mScanQuantity);
//        chkScanLager.setChecked(mScanLager);
    }

    void loadPrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mUseLagerort= sharedPref.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_LAGERORT_ENABLED, false);
        mScanLager=sharedPref.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_SCAN_LAGERORT_ENABLED, false);
        mScanQuantity=sharedPref.getBoolean(ScanAndCountSettingsActivity.KEY_PREF_SCAN_MENGE_ENABLED,false);

        //READ
//        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
//        Boolean defaultValue = true;
//        mUseLagerort = sharedPref.getBoolean(USE_LAGERORT, defaultValue);;
//        mScanLager=sharedPref.getBoolean(USE_SCANLAGER, true);
//        mScanQuantity=sharedPref.getBoolean(USE_SCANQUANTITY, false);
    }

//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        // Save the user's current game state
//        savedInstanceState.putBoolean(USE_LAGERORT, mUseLagerort);
//        savedInstanceState.putBoolean(USE_SCANLAGER, mScanLager);
//        savedInstanceState.putBoolean(USE_SCANQUANTITY, mScanQuantity);
//
//        // Always call the superclass so it can save the view hierarchy state
//        super.onSaveInstanceState(savedInstanceState);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //savePrefs();
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (barcodeReader != null) {
            // close BarcodeReader to clean up resources.
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }

    @Override
    public void onResume() {  //will always? be called before app becomes visible?
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
                Log.d(TAG, "scanner claimed");
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            // release BarcodeReader to clean up resources.
            barcodeReader.release();
        }

//        if (manager != null) {
//            // close AidcManager to disconnect from the scanner service.
//            // once closed, the object can no longer be used.
//            manager.close();
//        }
    }

    void doBeepWarn(){
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        try {
            barcodeReader.softwareTrigger(false);
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "barcodereader:  " + event.getBarcodeData());

        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String barcodeData = event.getBarcodeData();
//                String timestamp = event.getTimestamp();
//                // update UI to reflect the data
//                String s = (String) textView.getText();
//                s = barcodeData + "\n" + timestamp;
                Log.d(TAG, "onBarcodeEvent " + currentState.toString());
                switch (currentState) {
                    case data:
                        txtCurrent.setText(barcodeData);
                        txtQuantity.requestFocus();
                        break;
                    case quantity:
                        if(!mScanQuantity) {
                            doBeepWarn();
                            break;
                        }
                        //NUMBERS ONLY!
                        if (isNumeric(barcodeData)) {
                            txtQuantity.setText(barcodeData);
                            if (mUseLagerort)
                                txtLagerort.requestFocus();
                            else {
                                saveData();
                                startOver();
                            }
                        } else {
                            showToastMsg("Invalid Number");
                            blinkEditText(txtQuantity);
                            txtQuantity.requestFocus();
                        }
                        break;
                    case lager:
                        if(!mScanLager) {
                            doBeepWarn();
                            break;
                        }
                        txtLagerort.setText(barcodeData);
                        saveData();
                        startOver();
                        break;
                    default:
                        startOver();
                        break;
                }
//                txtData.setText(barcodeData);
//                txtQuantity.requestFocus();
            }
        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent arg0) {
        // TODO Auto-generated method stub
        try {
            barcodeReader.softwareTrigger(false);
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }

    }

    private void showToastMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //handle EditText focusChanges
    private View.OnFocusChangeListener onFocusChange = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            switch (v.getId()) {
                case R.id.txtData:
                    if (hasFocus) {
                        txtCurrent = txtData;
                        currentState = state.data;
                    }
                    if (hasFocus) {
                        txtData.setBackgroundColor(getResources().getColor(android.R.color.white));
                        hideKeyboard();
                        //txtData.setSelection(0, txtData.getText().length());
                    } else
                        txtData.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    break;
                case R.id.txtQuantity:
                    if (hasFocus) {
                        txtCurrent = txtQuantity;
                        currentState = state.quantity;
                    }
                    if (hasFocus) {
                        txtQuantity.setBackgroundColor(getResources().getColor(android.R.color.white));
                        txtQuantity.setSelection(0, txtQuantity.getText().length());
                        if(!mScanQuantity)
                            showKeyboard();
                    } else {
                        txtQuantity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        hideKeyboard();
                    }
                    break;
                case R.id.txtLagerort:
                    if (hasFocus) {
                        txtCurrent = txtLagerort;
                        currentState = state.lager;
                    }
                    if (hasFocus) {
                        txtLagerort.setBackgroundColor(getResources().getColor(android.R.color.white));
                        txtLagerort.setSelection(0, txtLagerort.getText().length());
                        showKeyboard();
                        txtCurrent = txtLagerort;
                    } else {
                        txtLagerort.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        hideKeyboard();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    //handle checkbox clicks
    private  View.OnClickListener myCheckBoxListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.chkUseLagerort:
                    if (((CheckBox) v).isChecked()) {
                        mUseLagerort = true;
                    } else {
                        mUseLagerort = false;
                    }
                    showHideLagerort();
                    break;
                case R.id.chkScanQuantity:
                    if (((CheckBox) v).isChecked()) {
                        mScanQuantity = true;
                    } else {
                        mScanQuantity = false;
                    }
                    break;
                case R.id.chkScanLager:
                    if (((CheckBox) v).isChecked()) {
                        mScanLager = true;
                    } else {
                        mScanLager = false;
                    }
                    break;
                default:
                    break;
            }

        }
    };

    //handle button clicks
    private View.OnClickListener myOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnSave:
                    saveData();
                    break;
                case R.id.btnList:
                    listData();
                    break;
                case R.id.btnClear:
                    clearData();
                    break;
                case R.id.btnExport:
                    exportData();
                    break;
                case R.id.btnKeybd:
                    if (bIsKeydbVisible)
                        hideKeyboard();
                    else
                        showKeyboard();
                    break;
                default:
                    break;
            }
        }
    };

    void exportData() {
        CSVHandler csvHandler = new CSVHandler(getApplicationContext());
        List<item> myItems = DataReaderDBHelper.readData(mDbHelper);
        String s = csvHandler.write(myItems);
        showToastMsg("Exported to: " + s);
    }

    void blinkEditText(final EditText et) {
        final ObjectAnimator backgroundColorAnimator = ObjectAnimator.ofObject(et,
                "backgroundColor",
                new ArgbEvaluator(),
                0xFFFFFFFF,
                0xffFF9696);
        backgroundColorAnimator.setDuration(300);
        backgroundColorAnimator.start();
    }

    boolean saveData() {
        //save the data
//        ContentValues values = new ContentValues();
        if (mUseLagerort == false) {
            if (txtData.getText().length() == 0 || txtQuantity.getText().length() == 0) {
                if (txtQuantity.getText().length() == 0) {
                    txtQuantity.requestFocus();
                    blinkEditText(txtQuantity);
                }
                if (txtData.getText().length() == 0) {
                    blinkEditText(txtData);
                    txtData.requestFocus();
                }
                return false;
            }
        } else {
            if (txtData.getText().length() == 0 || txtQuantity.getText().length() == 0 || txtLagerort.getText().length() == 0) {
                if (txtQuantity.getText().length() == 0) {
                    txtQuantity.requestFocus();
                    blinkEditText(txtQuantity);
                }
                if (txtData.getText().length() == 0) {
                    blinkEditText(txtData);
                    txtData.requestFocus();
                }
                if (txtLagerort.getText().length() == 0) {
                    txtLagerort.requestFocus();
                    blinkEditText(txtLagerort);
                }
                return false;
            }
        }

        String data = txtData.getText().toString();
        long count = Long.parseLong(txtQuantity.getText().toString());
        String lager = "0";
        if (mUseLagerort)
            lager = txtLagerort.getText().toString();
        Date date = new Date();
        item myItem = new item(data, lager, count, System.currentTimeMillis());
        DataReaderDBHelper.saveData(this.mDbHelper, myItem);
        showToastMsg("SAVED");
        startOver();
        return true;
    }

    void listData() {
        List<item> myItems = DataReaderDBHelper.readData(mDbHelper);
        // Appending records to a string buffer
        StringBuffer buffer = new StringBuffer();
        // Displaying all records
        for (item i : myItems) {
            buffer.append(
                    i.getData() + "\n" +
                            i.getQuantity() + "\n" +
                            i.getLagerort() + "\n" +
                            DataReaderDBHelper.getDateString(i.getTimestamp()) + "\n\n");
        }
        showMessage("Barcode Daten", buffer.toString());
    }

    void clearData() {
        //ask before delete!?
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String title = getResources().getString(R.string.txtYesNoDialogTitle);
        String txtYes = getResources().getString(R.string.txtYesNoDialogYesButtonText);
        String txtNo = getResources().getString(R.string.txtYesNoDialogNoButtonText);
        String text = getResources().getString(R.string.txtYesNoDialogText);
        builder.setTitle(title);
        builder.setMessage(text);

        builder.setPositiveButton(txtYes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                mDbHelper.clearData(mDbHelper);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.txtMessageYesDeleted), Toast.LENGTH_LONG);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(txtNo, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.txtMessageNoDelete), Toast.LENGTH_LONG);
                // Do nothing
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    void startOver() {
        txtData.setText("");
        txtQuantity.setText("");
        txtLagerort.setText("");
        txtData.requestFocus();
        hideKeyboard();
        //btnSave.setEnabled(false);

    }

    void hideKeyboard() {
        //hide keyboard
//        txtData.setInputType(InputType.TYPE_NULL);
//        txtData.setRawInputType(InputType.TYPE_CLASS_TEXT);
        /* hide keyboard */
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                .toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    void showKeyboard() {
        /* show keyboard */
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                .toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }

    public static boolean isNumeric(String str) {
        boolean b = str.matches("\\d+?");  //match a number no - or decimal point allowed
        if (b)
            Log.d(TAG, str + " is numeric");
        else
            Log.d(TAG, str + " is NOT numeric");
        return b;
    }
}
