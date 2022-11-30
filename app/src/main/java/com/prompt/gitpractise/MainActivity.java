package com.prompt.gitpractise;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.prompt.gitpractise.databinding.ActivityMainBinding;
import com.prompt.promptserialportcommunication.DLog;
import com.prompt.promptserialportcommunication.PromptUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String milkAnalyserDataSerial = "", weightScaleDataSerial = "";
    private int outputTextLength = 7;
    private double mFat = -1, mSnf = -1, mWater = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //For set configurations
        PromptUtils.setPortZeroConfigData(2400, 8, 1, 0, false, 84);
        PromptUtils.setPortOneConfigData(2400, 8, 1, 0, false, 84);
        PromptUtils.setPortTwoConfigData(9600, 8, 1, 0, false, 32);
        PromptUtils.setPortThreeConfigData(9600, 8, 1, 0, false, 10);

        //App crashing due to permission asking first time and ftdev getting null, so initially added start and stop
        PromptUtils.startPorts(this, true, true, true, true);
        PromptUtils.stopPorts();

        //For show and hide log in console
        DLog.showLog(false);

        //For start port
        binding.btnStart.setOnClickListener(v -> PromptUtils.startPorts(this, true, true, true, true));

        //For close port
        binding.btnStop.setOnClickListener(v -> PromptUtils.stopPorts());

        //For tare all ports
        binding.btnTareAll.setOnClickListener(v -> {
            PromptUtils.tarePortZero();
            PromptUtils.tarePortOne();
        });

        PromptUtils.mReceivedDataPortZero.observe(this, s -> {
            Log.e("Port 0 ", ", Value - " + s);
            setWeightScaleData(s);
        });

        PromptUtils.mReceivedDataPortOne.observe(this, s -> {
            Log.e("Port 1 ", ", Value - " + s);
            setMilkAnalyserData(s);
        });

        //Send Data to Printer
        binding.btnSendData.setOnClickListener(v -> {
            String printData = "Hello.." +
                    "\n" +
                    "Namskar.." +
                    "\n" +
                    "Prompt Equipment Pvt Ltd" +
                    "\n" +
                    "Thank you.." +
                    "\n" +
                    "\n";

            PromptUtils.writeDataOnSerial(printData, 3);
        });

        //Set Printer index - Default it is 3
        PromptUtils.printerPortIndex = 3;

        //Send data to display
        binding.btnSendDataToDisplay.setOnClickListener(v -> PromptUtils.sendDataToPortTwo("D00000100000000B00000000000000"));
        binding.btnSendDataToDisplay2.setOnClickListener(v -> PromptUtils.sendDataToPortTwo("D001202050065B08604050062080CRLF"));
    }

    private void setWeightScaleData(String s) {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
        Matcher matcher = pattern.matcher(s.trim());
        boolean isStringContainsSpecialCharacter = matcher.find();
        if (!isStringContainsSpecialCharacter) {
            if (s.trim().startsWith("L") && s.trim().length() == outputTextLength) {
                weightScaleDataSerial = s.trim();
                handleWeightScaleData(weightScaleDataSerial);
                weightScaleDataSerial = "";
            } else if (weightScaleDataSerial.length() < outputTextLength && !weightScaleDataSerial.equals("")) {
                weightScaleDataSerial = weightScaleDataSerial + s.trim();
                if (weightScaleDataSerial.length() == outputTextLength) {
                    handleWeightScaleData(weightScaleDataSerial);
                    weightScaleDataSerial = "";
                }
            } else {
                weightScaleDataSerial = "";
            }
        }
    }

    private void handleWeightScaleData(String value) {
        if (value.length() == outputTextLength) {
            String _value = value.substring(1, 7);
            binding.txtPortZeroData.setText("Weight Machine : " + Double.parseDouble(_value) / 100.0);
        }
    }

    private void setMilkAnalyserData(String s) {
        if (s.contains("(")) {
            milkAnalyserDataSerial = s;
        } else if (s.contains(")")) {
            milkAnalyserDataSerial = milkAnalyserDataSerial + s;
            try {
                handleMilkAnalyserDataFromSerial(milkAnalyserDataSerial);
            } catch (Exception e) {
                e.printStackTrace();
            }
            milkAnalyserDataSerial = "";
        } else {
            milkAnalyserDataSerial = milkAnalyserDataSerial + s;
        }
    }

    private void handleMilkAnalyserDataFromSerial(String value) {
        if (value.contains("(") && value.contains(")")) {
            String data = value;
            for (int i = 0; i <= 2; i++) {
                if (i == 0) {
                    String _value = data.substring(1, 4);
                    mFat = Double.parseDouble(_value) / 10.0;
                } else if (i == 1) {
                    String _value = data.substring(5, 8);
                    mSnf = Double.parseDouble(_value) / 10.0;
                } else if (i == 2) {
                    String _value = data.substring(13, 17);
                    mWater = Double.parseDouble(_value) / 100.0;
                }
            }
            binding.txtPortOneData.setText("Milk Analyser : FAT - " + mFat + " SNF - " + mSnf + " WATER - " + mWater);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Stop All Port while destroy activity
        PromptUtils.stopPorts();
    }
}