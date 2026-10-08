package com.example.cropsage;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // --- CONFIGURATION ---
    private static final String API_URL = "https://suggest.cropsage.qzz.io/api/v1/predict";
    private static final String API_KEY = "crop-sage-secret-2025";
    private static final String ESP32_NAME = "CropSage_Sensor";

    // Bluetooth Service
    private BluetoothService bluetoothService;

    // Inputs
    EditText etNitrogen, etPhosphorus, etPotassium, etPh, etTemp, etHumidity;
    Spinner spinnerMonth, spinnerSoil, spinnerLang;

    // UI Labels
    TextView lblInputTitle, lblNitrogen, lblPhosphorus, lblPotassium, lblMonth, lblPh, lblTemp, lblHumidity, lblSoil;
    TextView lblOutputTitle;

    // Outputs
    LinearLayout layoutResultCard, rowCrop1, rowCrop2;
    View divider1, divider2;
    TextView tvCrop1Name, tvCrop1Percent, tvCrop2Name, tvCrop2Percent;
    TextView tvWaterSource, tvCropDuration, tvHarvested;

    Button btnPredict, btnCopy, btnDownload, btnConnectSensor, btnFetchData;

    // Translation State
    int currentLangIndex = 0; // 0=En, 1=Hi, 2=Bn, 3=Ta
    Map<String, String[]> cropTranslations = new HashMap<>();

    // --- MASTER DATA ARRAYS ---
    final String[] monthsEng = {"Select month", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    final String[] monthsHin = {"महीना चुनें", "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"};
    final String[] monthsBen = {"মাস নির্বাচন", "জানুয়ারী", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};
    final String[] monthsTam = {"மாதம்", "ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்"};

    final String[] soilsEng = {
            "Select soil", "sandy loamy soil", "loamy soil", "well-drained soil", "laterite soil",
            "sandy soil", "clay loamy soil", "well-drained loamy soil", "alluvial soil",
            "clay soil", "red soil", "heavy soil", "black soil", "salty clay loamy soil"
    };
    final String[] soilsHin = {
            "मिट्टी चुनें", "बलुई दोमट मिट्टी", "दोमट मिट्टी", "अच्छी जल निकासी वाली मिट्टी", "लेटराइट मिट्टी",
            "रेतीली मिट्टी", "चिकनी दोमट मिट्टी", "अच्छी तरह से सूखी दोमट", "जलोढ़ मिट्टी",
            "चिकनी मिट्टी", "लाल मिट्टी", "भारी मिट्टी", "काली मिट्टी", "नमकीन चिकनी दोमट"
    };
    final String[] soilsBen = {
            "মাটি নির্বাচন", "বেলে দোআঁশ মাটি", "দোআঁশ মাটি", "সুনিষ্কাশিত মাটি", "ল্যাটেরাইট মাটি",
            "বেলে মাটি", "কাদা দোআঁশ মাটি", "সুনিষ্কাশিত দোআঁশ", "পলি মাটি",
            "কাদা মাটি", "লাল মাটি", "ভারী মাটি", "কালো মাটি", "লবনাক্ত কাদা দোআঁশ"
    };
    final String[] soilsTam = {
            "மண் வகை", "மணல் கலந்த வண்டல்", "வண்டல் மண்", "நன்கு வடிகட்டிய மண்", "செம்பூரான் மண்",
            "மணல் மண்", "களிமண் வண்டல்", "நன்கு வடிகட்டிய வண்டல்", "வண்டல் மண் (ஆற்று)",
            "களிமண்", "செம்மண்", "கனமான மண்", "கரிசல் மண்", "உவர் களிமண் வண்டல்"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Bluetooth Service
        bluetoothService = new BluetoothService();

        initializeViews();
        setupCropTranslations();
        setupSpinners();
        setupButtons();
    }

    private void initializeViews() {
        etNitrogen = findViewById(R.id.etNitrogen);
        etPhosphorus = findViewById(R.id.etPhosphorus);
        etPotassium = findViewById(R.id.etPotassium);
        etPh = findViewById(R.id.etPh);
        etTemp = findViewById(R.id.etTemp);
        etHumidity = findViewById(R.id.etHumidity);

        lblInputTitle = findViewById(R.id.lblInputTitle);
        lblNitrogen = findViewById(R.id.lblNitrogen);
        lblPhosphorus = findViewById(R.id.lblPhosphorus);
        lblPotassium = findViewById(R.id.lblPotassium);
        lblMonth = findViewById(R.id.lblMonth);
        lblPh = findViewById(R.id.lblPh);
        lblTemp = findViewById(R.id.lblTemp);
        lblHumidity = findViewById(R.id.lblHumidity);
        lblSoil = findViewById(R.id.lblSoil);
        lblOutputTitle = findViewById(R.id.lblOutputTitle);

        layoutResultCard = findViewById(R.id.layoutResultCard);
        rowCrop1 = findViewById(R.id.rowCrop1);
        rowCrop2 = findViewById(R.id.rowCrop2);
        divider1 = findViewById(R.id.divider1);
        divider2 = findViewById(R.id.divider2);

        tvCrop1Name = findViewById(R.id.tvCrop1Name);
        tvCrop1Percent = findViewById(R.id.tvCrop1Percent);
        tvCrop2Name = findViewById(R.id.tvCrop2Name);
        tvCrop2Percent = findViewById(R.id.tvCrop2Percent);
        tvWaterSource = findViewById(R.id.tvWaterSource);
        tvCropDuration = findViewById(R.id.tvCropDuration);
        tvHarvested = findViewById(R.id.tvHarvested);

        btnPredict = findViewById(R.id.btnPredict);
        btnCopy = findViewById(R.id.btnCopy);
        btnDownload = findViewById(R.id.btnDownload);

        // Spinners (FIXED: Added missing initialization)
        spinnerLang = findViewById(R.id.spinnerLang);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerSoil = findViewById(R.id.spinnerSoil);

        // BLUETOOTH BUTTONS
        btnConnectSensor = findViewById(R.id.btnConnectSensor);
        btnFetchData = findViewById(R.id.btnFetchData);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapterLang = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"English", "हिंदी", "বাংলা", "தமிழ்"});
        adapterLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLang.setAdapter(adapterLang);

        spinnerLang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLangIndex = position;
                updateStaticUIText(position);
                updateDropdownContent(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initialize Month/Soil with English default so they are never null
        updateDropdownContent(0);
    }

    private void updateDropdownContent(int langIndex) {
        int currentMonthPos = 0;
        int currentSoilPos = 0;

        // Safe check to avoid crash if called before initialization
        if (spinnerMonth != null && spinnerMonth.getSelectedItemPosition() >= 0) {
            currentMonthPos = spinnerMonth.getSelectedItemPosition();
        }
        if (spinnerSoil != null && spinnerSoil.getSelectedItemPosition() >= 0) {
            currentSoilPos = spinnerSoil.getSelectedItemPosition();
        }

        String[] targetMonths;
        String[] targetSoils;

        switch (langIndex) {
            case 1: targetMonths = monthsHin; targetSoils = soilsHin; break;
            case 2: targetMonths = monthsBen; targetSoils = soilsBen; break;
            case 3: targetMonths = monthsTam; targetSoils = soilsTam; break;
            default: targetMonths = monthsEng; targetSoils = soilsEng; break;
        }

        ArrayAdapter<String> adapterMonth = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targetMonths);
        adapterMonth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Ensure spinnerMonth is found if not already
        if (spinnerMonth == null) spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerMonth.setAdapter(adapterMonth);
        spinnerMonth.setSelection(currentMonthPos);

        ArrayAdapter<String> adapterSoil = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targetSoils);
        adapterSoil.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Ensure spinnerSoil is found if not already
        if (spinnerSoil == null) spinnerSoil = findViewById(R.id.spinnerSoil);
        spinnerSoil.setAdapter(adapterSoil);
        spinnerSoil.setSelection(currentSoilPos);
    }

    private void updateStaticUIText(int lang) {
        String[] tInput = {"Input Conditions", "इनपुट जानकारी", "ইনপুট তথ্য", "உள்ளீட்டு தகவல்"};
        String[] tN = {"Nitrogen (N)", "नाइट्रोजन (N)", "নাইট্রোজেন (N)", "நைட்ரஜன் (N)"};
        String[] tP = {"Phosphorus (P)", "फॉस्फोरस (P)", "ফসফরাস (P)", "பாஸ்பரஸ் (P)"};
        String[] tK = {"Potassium (K)", "पोटैशियम (K)", "পটাশিয়াম (K)", "பொட்டாசியம் (K)"};
        String[] tMonth = {"Sown Month", "बोने का महीना", "বপনের মাস", "விதைத்த மாதம்"};
        String[] tPh = {"Soil pH", "मिट्टी का pH", "মাটির pH", "மண் pH"};
        String[] tTemp = {"Temperature (°C)", "तापमान (°C)", "তাপমাত্রা (°C)", "வெப்பநிலை (°C)"};
        String[] tHum = {"Relative Humidity (%)", "आर्द्रता (%)", "আর্দ্রতা (%)", "ஈரப்பதம் (%)"};
        String[] tSoil = {"Soil Type", "मिट्टी का प्रकार", "মাটির ধরন", "மண் வகை"};
        String[] tBtnPred = {"Predict", "पूर्वानुमान", "ফলাফল দেখুন", "முன்னறிவு"};
        String[] tBtnDown = {"Download PDF", "पीडीएफ डाउनलोड", "পিডিএফ ডাউনলোড", "PDF பதிவிறக்கு"};
        String[] tBtnCopy = {"Copy Result", "कॉपी करें", "কপি করুন", "நகலெடு"};
        String[] tOutput = {"Prediction Output", "फसल सुझाव", "ফসলের সুপারিশ", "பயிர் பரிந்துரை"};

        lblInputTitle.setText(tInput[lang]);
        lblNitrogen.setText(tN[lang]);
        lblPhosphorus.setText(tP[lang]);
        lblPotassium.setText(tK[lang]);
        lblMonth.setText(tMonth[lang]);
        lblPh.setText(tPh[lang]);
        lblTemp.setText(tTemp[lang]);
        lblHumidity.setText(tHum[lang]);
        lblSoil.setText(tSoil[lang]);
        btnPredict.setText(tBtnPred[lang]);
        btnDownload.setText(tBtnDown[lang]);
        btnCopy.setText(tBtnCopy[lang]);
        lblOutputTitle.setText(tOutput[lang]);
    }

    private void setupCropTranslations() {
        cropTranslations.put("rice", new String[]{"धान", "ধান", "நெல்"});
        cropTranslations.put("maize", new String[]{"मक्का", "ভুট্টা", "மக்காச்சோளம்"});
        cropTranslations.put("wheat", new String[]{"गेहूं", "গম", "கோதுமை"});
        cropTranslations.put("pearl millet", new String[]{"बाजरा", "বাজরা", "கம்பு"});
        cropTranslations.put("sorghum", new String[]{"ज्वार", "জোয়ার", "சோளம்"});
        cropTranslations.put("ragi", new String[]{"रागी", "রাগি", "கேழ்வரகு"});
        cropTranslations.put("fingermillet", new String[]{"रागी", "রাগি", "கேழ்வரகு"});

        cropTranslations.put("chickpea", new String[]{"चना", "ছোলা", "கொண்டைக்கடலை"});
        cropTranslations.put("bengalgram", new String[]{"चना", "ছোলা", "கடலை"});
        cropTranslations.put("kidneybeans", new String[]{"राजमा", "রাজমা", "சிவப்பு காராமணி"});
        cropTranslations.put("pigeonpeas", new String[]{"अरहर", "তুর ডাল", "துவரை"});
        cropTranslations.put("redgram", new String[]{"अरहर", "আরহার ডাল", "துவரை"});
        cropTranslations.put("mothbeans", new String[]{"मोठ दाल", "মথ ডাল", "நரிப் பயறு"});
        cropTranslations.put("mungbean", new String[]{"मूंग", "মুগ ডাল", "பாசிப்பயறு"});
        cropTranslations.put("greengram", new String[]{"मूंग", "মুগ ডাল", "பாசிப்பயறு"});
        cropTranslations.put("blackgram", new String[]{"उड़द", "মাষকলাই", "உளுந்து"});
        cropTranslations.put("lentil", new String[]{"मसूर", "মসুর ডাল", "மைசூர் பருப்பு"});
        cropTranslations.put("horsegram", new String[]{"कुल्थी", "কুলথি", "கொள்ளு"});
        cropTranslations.put("cowpea", new String[]{"लोबिया", "বরবটি", "கராமணி"});

        cropTranslations.put("ribbed gourd", new String[]{"तुरई", "ঝিঙে", "பீர்க்கங்காய்"});
        cropTranslations.put("cucumber", new String[]{"खीरा", "শসা", "வெள்ளரிக்காய்"});
        cropTranslations.put("ash gourd", new String[]{"पेठा", "চালকুমড়া", "பூசணிக்காய்"});
        cropTranslations.put("pumpkin", new String[]{"कद्दू", "কুমড়া", "பரங்கிக்காய்"});
        cropTranslations.put("bottle gourd", new String[]{"लौकी", "লাউ", "சுரைக்காய்"});
        cropTranslations.put("bitter gourd", new String[]{"करेला", "করলা", "பாகற்காய்"});
        cropTranslations.put("snake gourd", new String[]{"चिचिंडा", "চিচিঙ্গা", "புடலங்காய்"});
        cropTranslations.put("bhendi", new String[]{"भिंडी", "ঢেঁড়স", "வெண்டைக்காய்"});
        cropTranslations.put("okra", new String[]{"भिंडी", "ঢেঁড়স", "வெண்டைக்காய்"});
        cropTranslations.put("brinjal", new String[]{"बैंगन", "বেগুন", "கத்திரிக்காய்"});
        cropTranslations.put("tomato", new String[]{"टमाटर", "টমেটো", "தக்காளி"});
        cropTranslations.put("cabbage", new String[]{"पत्तागोभी", "বাঁধাকপি", "முட்டைக்கோஸ்"});
        cropTranslations.put("cauliflower", new String[]{"फूलगोभी", "ফুলকপি", "பூகோசு"});
        cropTranslations.put("carrot", new String[]{"गाजर", "গাজর", "கேரட்"});
        cropTranslations.put("radish", new String[]{"मूली", "মূলা", "முள்ளங்கி"});
        cropTranslations.put("beetroot", new String[]{"चुकंदर", "বিট", "பீட்ரூட்"});
        cropTranslations.put("onion", new String[]{"प्याज़", "পেঁয়াজ", "வெங்காயம்"});
        cropTranslations.put("small onion", new String[]{"छोटा प्याज़", "ছোট পেঁয়াজ", "சின்ன வெங்காயம்"});
        cropTranslations.put("chillies", new String[]{"मिर्च", "লঙ্কা", "மிளகாய்"});
        cropTranslations.put("french bean", new String[]{"फ्रेंच बीन्स", "ফরাসি শিম", "பீன்ஸ்"});
        cropTranslations.put("cluster bean", new String[]{"ग्वार", "গোয়ার শিম", "கொத்தவரங்காய்"});
        cropTranslations.put("capsicum", new String[]{"शिमला मिर्च", "ক্যাপসিকাম", "குடைமிளகாய்"});
        cropTranslations.put("tinda", new String[]{"टिंडा", "টিন্ডা", "டிண்டா"});
        cropTranslations.put("chowchow", new String[]{"चायोट", "চাউ চাউ", "சௌ சௌ"});
        cropTranslations.put("vegetable cowpea", new String[]{"सब्जी लोबिया", "সবজি বরবটি", "காய் கராமணி"});

        cropTranslations.put("watermelon", new String[]{"तरबूज", "তরমুজ", "தர்பூசணி"});
        cropTranslations.put("muskmelon", new String[]{"खरबूजा", "খরমুজ", "முலாம்பழம்"});
        cropTranslations.put("apple", new String[]{"सेब", "আপেল", "ஆப்பிள்"});
        cropTranslations.put("orange", new String[]{"संतरा", "কমলা", "ஆரஞ்சு"});
        cropTranslations.put("papaya", new String[]{"पपीता", "পেঁপে", "பப்பாளி"});
        cropTranslations.put("pomegranate", new String[]{"अनार", "ডালিম", "மாதுளை"});
        cropTranslations.put("banana", new String[]{"केला", "কলা", "வாழை"});
        cropTranslations.put("mango", new String[]{"आम", "আম", "மாம்பழம்"});
        cropTranslations.put("grapes", new String[]{"अंगूर", "আঙ্গুর", "திராட்சை"});

        cropTranslations.put("cotton", new String[]{"कपास", "তুলা", "பருத்தி"});
        cropTranslations.put("jute", new String[]{"जूट", "পাট", "சணல்"});
        cropTranslations.put("coffee", new String[]{"कॉफी", "কফি", "காபி"});
        cropTranslations.put("coconut", new String[]{"नारियल", "নারকেল", "தேங்காய்"});
        cropTranslations.put("sugarcane", new String[]{"गन्ना", "आख", "கரும்பு"});
        cropTranslations.put("sunflower", new String[]{"सूरजमुखी", "সূর্যমুখী", "சூரியகாந்தி"});
        cropTranslations.put("groundnut", new String[]{"मूंगफली", "চিনাবাদাম", "நிலக்கடலை"});
        cropTranslations.put("castor", new String[]{"अरंडी", "रेड़ी", "ஆமணக்கு"});
        cropTranslations.put("gingely", new String[]{"तिल", "তিল", "எள்"});
        cropTranslations.put("soyabean", new String[]{"सोयाबीन", "সয়াবিন", "சோயாபீன்"});
        cropTranslations.put("tapoica", new String[]{"कसावा", "কাসাভা", "மரவள்ளிக்கிழங்கு"});
        cropTranslations.put("sweet potato", new String[]{"शकरकंद", "মিষ্টি আলু", "சர்க்கரைவள்ளிக்கிழங்கு"});
        cropTranslations.put("annual moringa", new String[]{"वार्षिक सहजन", "বার্ষিক সজনে", "முருங்கை"});
    }

    private String getTranslatedCrop(String cropName) {
        String lower = cropName.toLowerCase();
        if (currentLangIndex == 0) return capitalize(lower);
        if (cropTranslations.containsKey(lower)) {
            return cropTranslations.get(lower)[currentLangIndex - 1];
        }
        return capitalize(lower);
    }

    private void setupButtons() {
        btnPredict.setOnClickListener(v -> performPrediction());
        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnDownload.setOnClickListener(v -> downloadReport());

        // Connect to Sensor
        btnConnectSensor.setOnClickListener(v -> connectToSensor());

        // Fetch Data
        btnFetchData.setOnClickListener(v -> fetchSensorData());
    }

    // --- BLUETOOTH FUNCTIONS ---
    private void connectToSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return;
            }
        }

        btnConnectSensor.setText("Connecting...");
        new Thread(() -> {
            boolean success = bluetoothService.connectToSensor(ESP32_NAME);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
                    btnConnectSensor.setText("Connected");
                    btnConnectSensor.setBackgroundColor(0xFF00E676);
                    btnFetchData.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
                    btnConnectSensor.setText("Connect Sensor");
                }
            });
        }).start();
    }

    private void fetchSensorData() {
        Toast.makeText(this, "Fetching...", Toast.LENGTH_SHORT).show();
        bluetoothService.requestData(json -> {
            try {
                JSONObject data = new JSONObject(json);
                etNitrogen.setText(String.valueOf(data.getInt("N")));
                etPhosphorus.setText(String.valueOf(data.getInt("P")));
                etPotassium.setText(String.valueOf(data.getInt("K")));
                etPh.setText(String.valueOf(data.getDouble("SOIL_PH")));
                etTemp.setText(String.valueOf(data.getInt("TEMP")));
                etHumidity.setText(String.valueOf(data.getInt("RELATIVE_HUMIDITY")));
                Toast.makeText(this, "Sensors Read!", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid Data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performPrediction() {
        if (!validateInputs()) return;
        btnPredict.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("N", Double.parseDouble(etNitrogen.getText().toString()));
            jsonBody.put("P", Double.parseDouble(etPhosphorus.getText().toString()));
            jsonBody.put("K", Double.parseDouble(etPotassium.getText().toString()));
            jsonBody.put("SOIL_PH", Double.parseDouble(etPh.getText().toString()));
            jsonBody.put("TEMP", Double.parseDouble(etTemp.getText().toString()));
            jsonBody.put("RELATIVE_HUMIDITY", Double.parseDouble(etHumidity.getText().toString()));

            jsonBody.put("SOWN", spinnerMonth.getSelectedItemPosition());
            int soilIndex = spinnerSoil.getSelectedItemPosition();
            jsonBody.put("SOIL", soilsEng[soilIndex]);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("x-api-key", API_KEY);
                conn.setDoOutput(true);

                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    JSONObject responseJson = new JSONObject(response.toString());
                    runOnUiThread(() -> updateUI(responseJson));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Server Error: " + code, Toast.LENGTH_SHORT).show();
                        btnPredict.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                    btnPredict.setEnabled(true);
                });
            }
        }).start();
    }

    private void updateUI(JSONObject data) {
        try {
            layoutResultCard.setVisibility(View.VISIBLE);
            btnPredict.setEnabled(true);
            JSONArray recs = data.getJSONArray("recommendations");

            if(recs.length() > 0) {
                rowCrop1.setVisibility(View.VISIBLE);
                divider1.setVisibility(View.VISIBLE);
                JSONObject c1 = recs.getJSONObject(0);
                tvCrop1Name.setText(getTranslatedCrop(c1.getString("crop")));
                double score = c1.getDouble("confidence");
                tvCrop1Percent.setText(String.format("%.1f%%", score * 100));
                setConfidenceColor(tvCrop1Percent, score);
            } else {
                rowCrop1.setVisibility(View.GONE);
                divider1.setVisibility(View.GONE);
            }

            if(recs.length() > 1) {
                rowCrop2.setVisibility(View.VISIBLE);
                divider2.setVisibility(View.VISIBLE);
                JSONObject c2 = recs.getJSONObject(1);
                tvCrop2Name.setText(getTranslatedCrop(c2.getString("crop")));
                double score = c2.getDouble("confidence");
                tvCrop2Percent.setText(String.format("%.1f%%", score * 100));
                setConfidenceColor(tvCrop2Percent, score);
            } else {
                rowCrop2.setVisibility(View.GONE);
                divider2.setVisibility(View.GONE);
            }

            String[] tWater = {"Water Source: ", "पानी का स्रोत: ", "জলের উৎস: ", "நீரின் மூலம்: "};
            String[] tDur = {"Crop Duration: ", "फसल अवधि: ", "ফসলের সময়কাল: ", "பயிர் காலம்: "};
            String[] tHarv = {"Harvested: ", "उपज: ", "উৎপাদন: ", "மகசூல்: "};
            String[] tDays = {" days", " दिन", " দিন", " நாட்கள்"};
            int i = currentLangIndex;

            tvWaterSource.setText(tWater[i] + data.getString("water_source"));
            tvCropDuration.setText(tDur[i] + data.getDouble("crop_duration") + tDays[i]);
            tvHarvested.setText(tHarv[i] + data.getDouble("harvested"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setConfidenceColor(TextView view, double score) {
        if (score >= 0.7) {
            view.setBackgroundResource(R.drawable.bg_button_green);
            view.setTextColor(0xFF052E16);
        } else if (score >= 0.4) {
            view.setBackgroundResource(R.drawable.bg_button_orange);
            view.setTextColor(0xFF3B2F0B);
        } else {
            view.setBackgroundResource(R.drawable.bg_button_red);
            view.setTextColor(0xFFFFFFFF);
        }
    }

    private boolean validateInputs() {
        if (etNitrogen.getText().toString().isEmpty() || etPhosphorus.getText().toString().isEmpty() ||
                etPotassium.getText().toString().isEmpty() || etPh.getText().toString().isEmpty() ||
                etTemp.getText().toString().isEmpty() || etHumidity.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        Spinner sMonth = findViewById(R.id.spinnerMonth);
        if (sMonth.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select a month", Toast.LENGTH_SHORT).show();
            return false;
        }
        Spinner sSoil = findViewById(R.id.spinnerSoil);
        if (sSoil.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select a soil type", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String capitalize(String str) {
        if(str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String generateFullReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CROP SAGE REPORT ===\n\n");
        sb.append("[ INPUTS ]\n");
        sb.append("N: ").append(etNitrogen.getText().toString()).append(" | P: ").append(etPhosphorus.getText().toString()).append(" | K: ").append(etPotassium.getText().toString()).append("\n");
        sb.append("pH: ").append(etPh.getText().toString()).append(" | Temp: ").append(etTemp.getText().toString()).append("C\n");
        sb.append("Month: ").append(spinnerMonth.getSelectedItem().toString()).append("\n");
        sb.append("Soil: ").append(spinnerSoil.getSelectedItem().toString()).append("\n\n");

        sb.append("[ RESULT ]\n");
        if(layoutResultCard.getVisibility() == View.VISIBLE) {
            sb.append("1. ").append(tvCrop1Name.getText()).append(" (").append(tvCrop1Percent.getText()).append(")\n");
            if(rowCrop2.getVisibility() == View.VISIBLE) {
                sb.append("2. ").append(tvCrop2Name.getText()).append(" (").append(tvCrop2Percent.getText()).append(")\n");
            }
            sb.append(tvWaterSource.getText()).append("\n");
            sb.append(tvCropDuration.getText()).append("\n");
        } else {
            sb.append("No prediction yet.\n");
        }
        return sb.toString();
    }

    private void copyToClipboard() {
        String fullReport = generateFullReportString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Crop Sage Report", fullReport));
        Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void downloadReport() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(14);

        int x = 40;
        int y = 60;

        canvas.drawText("Crop Sage Report", x, y, titlePaint);
        y += 50;

        paint.setFakeBoldText(true);
        canvas.drawText("Input Conditions:", x, y, paint);
        paint.setFakeBoldText(false);
        y += 30;

        canvas.drawText("Nitrogen: " + etNitrogen.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Phosphorus: " + etPhosphorus.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Potassium: " + etPotassium.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("pH Level: " + etPh.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Temperature: " + etTemp.getText().toString() + " C", x, y, paint);
        y += 20;
        canvas.drawText("Humidity: " + etHumidity.getText().toString() + "%", x, y, paint);
        y += 20;
        canvas.drawText("Month: " + spinnerMonth.getSelectedItem().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Soil Type: " + spinnerSoil.getSelectedItem().toString(), x, y, paint);

        y += 50;
        paint.setFakeBoldText(true);
        canvas.drawText("Prediction Results:", x, y, paint);
        paint.setFakeBoldText(false);
        y += 30;

        if (layoutResultCard.getVisibility() == View.VISIBLE) {
            paint.setTextSize(16);
            paint.setColor(Color.parseColor("#006400"));
            canvas.drawText("1. " + tvCrop1Name.getText() + "  " + tvCrop1Percent.getText(), x, y, paint);
            y += 30;

            if(rowCrop2.getVisibility() == View.VISIBLE) {
                paint.setColor(Color.RED);
                canvas.drawText("2. " + tvCrop2Name.getText() + "  " + tvCrop2Percent.getText(), x, y, paint);
                y += 30;
            }

            paint.setColor(Color.BLACK);
            paint.setTextSize(14);
            y += 10;
            canvas.drawText(tvWaterSource.getText().toString(), x, y, paint);
            y += 20;
            canvas.drawText(tvCropDuration.getText().toString(), x, y, paint);
            y += 20;
            canvas.drawText(tvHarvested.getText().toString(), x, y, paint);
        } else {
            canvas.drawText("No prediction data available.", x, y, paint);
        }

        y += 60;
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("Generated by Crop Sage App", x, y, paint);

        document.finishPage(page);

        String fileName = "CropSage_" + System.currentTimeMillis() + ".pdf";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    document.writeTo(out);
                    document.close();
                    out.close();

                    Toast.makeText(this, "PDF Saved!", Toast.LENGTH_LONG).show();
                    openFile(uri);
                }
            } else {
                Toast.makeText(this, "Requires Android 10+", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Saved! Install PDF Viewer.", Toast.LENGTH_LONG).show();
        }
    }
}