package com.facetec.sampleapp;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.facetec.sdk.FaceTecSDK;
import com.facetec.sdk.FaceTecVocalGuidanceCustomization;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import Processors.Config;
import Processors.ThemeHelpers;

public class SampleAppUtilities {
    enum VocalGuidanceMode {
        OFF,
        MINIMAL,
        FULL
    }
    private MediaPlayer vocalGuidanceOnPlayer;
    private MediaPlayer vocalGuidanceOffPlayer;
    static SampleAppUtilities.VocalGuidanceMode vocalGuidanceMode = VocalGuidanceMode.MINIMAL;

    private SampleAppActivity sampleAppActivity;
    public String currentTheme = Config.wasSDKConfiguredWithConfigWizard ? "Config Wizard Theme" : "FaceTec Theme";
    private Handler themeTransitionTextHandler;

    public SampleAppUtilities(SampleAppActivity activity) {
        sampleAppActivity = activity;
    }

    public void setupAllButtons() {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.enrollButton.setupButton(sampleAppActivity);
                sampleAppActivity.activityMainBinding.authButton.setupButton(sampleAppActivity);
                sampleAppActivity.activityMainBinding.livenessCheckButton.setupButton(sampleAppActivity);
                sampleAppActivity.activityMainBinding.identityCheckButton.setupButton(sampleAppActivity);
                sampleAppActivity.activityMainBinding.auditTrailImagesButton.setupButton(sampleAppActivity);
                sampleAppActivity.activityMainBinding.settingsButton.setupButton(sampleAppActivity);
            }
        });
    }

    public void disableAllButtons() {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.enrollButton.setEnabled(false, true);
                sampleAppActivity.activityMainBinding.authButton.setEnabled(false, true);
                sampleAppActivity.activityMainBinding.livenessCheckButton.setEnabled(false, true);
                sampleAppActivity.activityMainBinding.identityCheckButton.setEnabled(false, true);
                sampleAppActivity.activityMainBinding.auditTrailImagesButton.setEnabled(false, true);
                sampleAppActivity.activityMainBinding.settingsButton.setEnabled(false, true);
            }
        });
    }

    public void enableAllButtons() {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.enrollButton.setEnabled(true, true);
                sampleAppActivity.activityMainBinding.authButton.setEnabled(true, true);
                sampleAppActivity.activityMainBinding.livenessCheckButton.setEnabled(true, true);
                sampleAppActivity.activityMainBinding.identityCheckButton.setEnabled(true, true);
                sampleAppActivity.activityMainBinding.auditTrailImagesButton.setEnabled(true, true);
                sampleAppActivity.activityMainBinding.settingsButton.setEnabled(true, true);
            }
        });
    }

    public void showSessionTokenConnectionText() {
        themeTransitionTextHandler = new Handler();
        themeTransitionTextHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionText.animate().alpha(1f).setDuration(600);
            }
        }, 3000);
    }

    public void hideSessionTokenConnectionText() {
        themeTransitionTextHandler.removeCallbacksAndMessages(null);
        themeTransitionTextHandler = null;
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionText.animate().alpha(0f).setDuration(600);
            }
        });
    }

    // Disable buttons to prevent hammering, fade out main interface elements, and shuffle the guidance images.
    public void fadeOutMainUIAndPrepareForFaceTecSDK(final Runnable callback) {
        disableAllButtons();
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.vocalGuidanceSettingButton.animate().alpha(0f).setDuration(600).start();
                sampleAppActivity.activityMainBinding.themeTransitionImageView.animate().alpha(1f).setDuration(600).start();
                sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(0f).setDuration(600).withEndAction(callback).start();
            }
        });
    }

    public void fadeInMainUI() {
        enableAllButtons();
        sampleAppActivity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  sampleAppActivity.activityMainBinding.vocalGuidanceSettingButton.animate().alpha(1f).setDuration(600);
                  sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(1f).setDuration(600);
                  sampleAppActivity.activityMainBinding.themeTransitionImageView.animate().alpha(0f).setDuration(600);
              }
            }
        );
    }

    public void displayStatus(final String statusString) {
        Log.d("FaceTecSDKSampleApp", statusString);
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.statusLabel.setText(statusString);
            }
        });
    }

    public void showAuditTrailImages() {
        // Store audit trail images from latest session result for inspection
        ArrayList<Bitmap> auditTrailAndIDScanImages = new ArrayList<>();
        if (sampleAppActivity.latestSessionResult != null) {
            // convert the compressed base64 encoded audit trail images into bitmaps
            for(String compressedBase64EncodedAuditTrailImage: sampleAppActivity.latestSessionResult.getAuditTrailCompressedBase64()) {
                byte[] decodedString = Base64.decode(compressedBase64EncodedAuditTrailImage, Base64.DEFAULT);
                Bitmap auditTrailImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                auditTrailAndIDScanImages.add(auditTrailImage);
            }
        }

        if(sampleAppActivity.latestIDScanResult != null && !sampleAppActivity.latestIDScanResult.getFrontImagesCompressedBase64().isEmpty()) {
            byte[] decodedString = Base64.decode(sampleAppActivity.latestIDScanResult.getFrontImagesCompressedBase64().get(0), Base64.DEFAULT);
            Bitmap frontImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            auditTrailAndIDScanImages.add(frontImage);
        }

        if(auditTrailAndIDScanImages.size() <= 0) {
            displayStatus("No audit trail images obtained");
            return;
        }

        for(int i = auditTrailAndIDScanImages.size() - 1; i >= 0; i--) {
            addDismissableImageToInterface(auditTrailAndIDScanImages.get(i));
        }
    }

    public void addDismissableImageToInterface(Bitmap imageBitmap) {
        final Dialog imageDialog = new Dialog(sampleAppActivity);
        imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        imageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView imageView = new ImageView(sampleAppActivity);
        imageView.setImageBitmap(imageBitmap);
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imageDialog.dismiss();
            }
        });

        // Scale image to better fit device's display.
        DisplayMetrics dm = new DisplayMetrics();
        sampleAppActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(Double.valueOf(dm.widthPixels * 0.5).intValue(), Double.valueOf(dm.heightPixels * 0.5).intValue());
        imageDialog.addContentView(imageView, layout);
        imageDialog.show();
    }

    public void handleErrorGettingServerSessionToken() {
        hideSessionTokenConnectionText();
        displayStatus("Session could not be started due to an unexpected issue during the network request.");
        fadeInMainUI();
    }

    public void showThemeSelectionMenu() {

        final String[] themes;
        if(Config.wasSDKConfiguredWithConfigWizard == true) {
            themes = new String[] { "Config Wizard Theme", "FaceTec Theme", "Pseudo-Fullscreen", "Well-Rounded", "Bitcoin Exchange", "eKYC", "Sample Bank"};
        }
        else {
            themes = new String[] {"FaceTec Theme", "Pseudo-Fullscreen", "Well-Rounded", "Bitcoin Exchange", "eKYC", "Sample Bank"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(sampleAppActivity, android.R.style.Theme_Holo_Light));
        builder.setTitle("Select a Theme:");
        builder.setItems(themes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                currentTheme = themes[index];
                ThemeHelpers.setAppTheme(sampleAppActivity, currentTheme);
                updateThemeTransitionView();
            }
        });
        builder.show();
    }

    public void updateThemeTransitionView() {
        int transitionViewImage = 0;
        int transitionViewTextColor = Config.currentCustomization.getGuidanceCustomization().foregroundColor;
        switch (currentTheme) {
            case "FaceTec Theme":
                break;
            case "Config Wizard Theme":
                break;
            case "Pseudo-Fullscreen":
                break;
            case "Well-Rounded":
                transitionViewImage = R.drawable.well_rounded_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            case "Bitcoin Exchange":
                transitionViewImage = R.drawable.bitcoin_exchange_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            case "eKYC":
                transitionViewImage = R.drawable.ekyc_bg;
                break;
            case "Sample Bank":
                transitionViewImage = R.drawable.sample_bank_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            default:
                break;
        }

        sampleAppActivity.activityMainBinding.themeTransitionImageView.setImageResource(transitionViewImage);
        sampleAppActivity.activityMainBinding.themeTransitionText.setTextColor(transitionViewTextColor);
    }

    void setUpVocalGuidancePlayers() {
        vocalGuidanceOnPlayer = MediaPlayer.create(sampleAppActivity, R.raw.vocal_guidance_on);
        vocalGuidanceOffPlayer = MediaPlayer.create(sampleAppActivity, R.raw.vocal_guidance_off);
        vocalGuidanceMode = SampleAppUtilities.VocalGuidanceMode.MINIMAL;
    }

    void setVocalGuidanceMode() {
        if(isDeviceMuted()) {
            AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(sampleAppActivity, android.R.style.Theme_Holo_Light)).create();
            alertDialog.setMessage("Vocal Guidance is disabled when the device is muted");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            alertDialog.show();
            return;
        }

        if(vocalGuidanceOnPlayer.isPlaying() || vocalGuidanceOffPlayer.isPlaying()) {
            return;
        }

        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(vocalGuidanceMode) {
                    case OFF:
                        vocalGuidanceMode = SampleAppUtilities.VocalGuidanceMode.MINIMAL;
                        sampleAppActivity.activityMainBinding.vocalGuidanceSettingButton.setImageResource(R.drawable.vocal_minimal);
                        vocalGuidanceOnPlayer.start();
                        Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.MINIMAL_VOCAL_GUIDANCE;
                        break;
                    case MINIMAL:
                        vocalGuidanceMode = SampleAppUtilities.VocalGuidanceMode.FULL;
                        sampleAppActivity.activityMainBinding.vocalGuidanceSettingButton.setImageResource(R.drawable.vocal_full);
                        vocalGuidanceOnPlayer.start();
                        Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.FULL_VOCAL_GUIDANCE;
                        break;
                    case FULL:
                        vocalGuidanceMode = SampleAppUtilities. VocalGuidanceMode.OFF;
                        sampleAppActivity.activityMainBinding.vocalGuidanceSettingButton.setImageResource(R.drawable.vocal_off);
                        vocalGuidanceOffPlayer.start();
                        Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.NO_VOCAL_GUIDANCE;
                        break;
                }

                SampleAppUtilities.setVocalGuidanceSoundFiles();
                FaceTecSDK.setCustomization(Config.currentCustomization);
            }
        });
    }

    public static void setVocalGuidanceSoundFiles() {
        Config.currentCustomization.vocalGuidanceCustomization.pleaseFrameYourFaceInTheOvalSoundFile = R.raw.please_frame_your_face_sound_file;
        Config.currentCustomization.vocalGuidanceCustomization.pleaseMoveCloserSoundFile = R.raw.please_move_closer_sound_file;
        Config.currentCustomization.vocalGuidanceCustomization.pleaseRetrySoundFile = R.raw.please_retry_sound_file;
        Config.currentCustomization.vocalGuidanceCustomization.uploadingSoundFile = R.raw.uploading_sound_file;
        Config.currentCustomization.vocalGuidanceCustomization.facescanSuccessfulSoundFile = R.raw.facescan_successful_sound_file;
        Config.currentCustomization.vocalGuidanceCustomization.pleasePressTheButtonToStartSoundFile = R.raw.please_press_button_sound_file;

        switch(vocalGuidanceMode) {
            case OFF:
                Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.NO_VOCAL_GUIDANCE;
                break;
            case MINIMAL:
                Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.MINIMAL_VOCAL_GUIDANCE;
                break;
            case FULL:
                Config.currentCustomization.vocalGuidanceCustomization.mode = FaceTecVocalGuidanceCustomization.VocalGuidanceMode.FULL_VOCAL_GUIDANCE;
                break;
        }
    }

    boolean isDeviceMuted() {
        AudioManager audio = (AudioManager) (sampleAppActivity.getSystemService(Context.AUDIO_SERVICE));
        if(audio.getStreamVolume(AudioManager.STREAM_MUSIC) ==  0) {
            return true;
        }
        else {
            return  false;
        }
    }

    public static void setOCRLocalization(Context context) {
        // Set the strings to be used for group names, field names, and placeholder texts for the FaceTec ID Scan User OCR Confirmation Screen.
        // DEVELOPER NOTE: For this demo, we are using the template json file, 'FaceTec_OCR_Customization.json,' as the parameter in calling this API.
        // For the configureOCRLocalization API parameter, you may use any object that follows the same structure and key naming as the template json file, 'FaceTec_OCR_Customization.json'.
        try {
            InputStream is = context.getAssets().open("FaceTec_OCR_Customization.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String ocrLocalizationJSONString = new String(buffer, "UTF-8");
            JSONObject ocrLocalizationJSON = new JSONObject(ocrLocalizationJSONString);

            FaceTecSDK.configureOCRLocalization(ocrLocalizationJSON);

        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }
    }
}
