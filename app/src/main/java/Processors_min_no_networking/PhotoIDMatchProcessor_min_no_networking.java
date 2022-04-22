// Welcome to the minimized FaceTec Device SDK code to launch User Sessions and retrieve 3D FaceScans (for further processing)!
// This file removes comment annotations, as well as networking calls,
// in an effort to demonstrate how little code is needed to get the FaceTec Device SDKs to work.

// NOTE: This example DOES NOT perform a secure Photo ID Scan. To perform a secure Photo ID Scan, you need to actually make an API call.
// Please see the PhotoIDScanProcessor file for a complete demonstration using the FaceTec Testing API.

package Processors_min_no_networking;

import android.content.Context;
import android.util.Log;

import com.facetec.sampleapp.SampleAppActivity;
import com.facetec.sdk.FaceTecFaceScanProcessor;
import com.facetec.sdk.FaceTecFaceScanResultCallback;
import com.facetec.sdk.FaceTecIDScanProcessor;
import com.facetec.sdk.FaceTecIDScanResult;
import com.facetec.sdk.FaceTecIDScanResultCallback;
import com.facetec.sdk.FaceTecIDScanStatus;
import com.facetec.sdk.FaceTecSessionActivity;
import com.facetec.sdk.FaceTecSessionResult;
import com.facetec.sdk.FaceTecSessionStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PhotoIDMatchProcessor_min_no_networking implements FaceTecFaceScanProcessor, FaceTecIDScanProcessor {
    private SampleAppActivity sampleAppActivity;

    public PhotoIDMatchProcessor_min_no_networking(final Context context, String sessionToken) {
        // Core FaceTec Device SDK code that starts the User Session.
        FaceTecSessionActivity.createAndLaunchSession(context, PhotoIDMatchProcessor_min_no_networking.this, PhotoIDMatchProcessor_min_no_networking.this, sessionToken);
    }

    public void processSessionWhileFaceTecSDKWaits(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback) {
        // Normally a User will complete a Session.  This checks to see if there was a cancellation, timeout, or some other non-success case.
        if(sessionResult.getStatus() != FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY) {
            faceScanResultCallback.cancel();
            return;
        }

        // IMPORTANT: FaceTecSDK.FaceTecSessionStatus.SessionCompletedSuccessfully DOES NOT mean the enrollment was Successful.
        // It simply means the User completed the Session and a 3D FaceScan was created. You still need to perform the enrollment on your Servers.

        // These are the core parameters
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("faceScan", sessionResult.getFaceScanBase64());
            parameters.put("auditTrailImage", sessionResult.getAuditTrailCompressedBase64()[0]);
            parameters.put("lowQualityAuditTrailImage", sessionResult.getLowQualityAuditTrailCompressedBase64()[0]);
            parameters.put("externalDatabaseRefID", sampleAppActivity.getLatestExternalDatabaseRefID());
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        // DEVELOPER TODOS:
        // 1.  Call your own API with the above data and pass into the Server SDK
        // 2.  If the Server SDK successfully processes the data, call proceedToNextStep(scanResultBlob), passing in the generated scanResultBlob to the parameter.
        //     If proceedToNextStep(scanResultBlob) returns as true, the FaceScan part of the Session was successful and will be proceeding to the ID Scan.
        //     If proceedToNextStep(scanResultBlob) returns as false, the Session will be proceeding to a retry of the FaceScan.
        // 3.  cancel() is provided in case you detect issues with your own API, such as errors processing and returning the scanResultBlob.
        // 4.  uploadProgress(yourUploadProgressFloat) is provided to control the Progress Bar.

        // faceScanResultCallback.proceedToNextStep(scanResultBlob)
        // faceScanResultCallback.cancel()
        // faceScanResultCallback.uploadProgress(yourUploadProgressFloat)
    }

    public void processIDScanWhileFaceTecSDKWaits(FaceTecIDScanResult idScanResult, final FaceTecIDScanResultCallback idScanResultCallback) {
        // Normally a User will complete a Session. This checks to see if there was a cancellation, timeout, or some other non-success case.
        if(idScanResult.getStatus() != FaceTecIDScanStatus.SUCCESS) {
            idScanResultCallback.cancel();
            return;
        }

        // IMPORTANT: FaceTecIDScanStatus.SUCCESS DOES NOT mean the ID Scan 3d-2d Matching was Successful.
        // It simply means the User completed the Session and a 3D FaceScan was created. You still need to perform the ID Scan 3d-2d Matching on your Servers.

        // These are the core parameters
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("externalDatabaseRefID", sampleAppActivity.getLatestExternalDatabaseRefID());
            parameters.put("idScan", idScanResult.getIDScanBase64());
            parameters.put("minMatchLevel", 3);

            ArrayList<String> frontImagesCompressedBase64 = idScanResult.getFrontImagesCompressedBase64();
            ArrayList<String> backImagesCompressedBase64 = idScanResult.getBackImagesCompressedBase64();
            if(frontImagesCompressedBase64.size() > 0) {
                parameters.put("idScanFrontImage", frontImagesCompressedBase64.get(0));
            }
            if(backImagesCompressedBase64.size() > 0) {
                parameters.put("idScanBackImage", backImagesCompressedBase64.get(0));
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        // DEVELOPER TODOS:
        // 1.  Call your own API with the above data and pass into the Server SDK
        // 2.  If the Server SDK successfully processes the data, call proceedToNextStep(scanResultBlob), passing in the generated scanResultBlob to the parameter.
        //     If proceedToNextStep(scanResultBlob) returns as true, the ID Scan part of the Session was successful and onActivityResult() will be called next.
        //     If proceedToNextStep(scanResultBlob) returns as false, the ID Scan Session is continuing to advance through the User Flow, passing back another Session Result once the next step in the User Flow is complete and ready to be processed by the Server SDK.
        // 3.  cancel() is provided in case you detect issues with your own API, such as errors processing and returning the scanResultBlob.
        // 4.  uploadProgress(yourUploadProgressFloat) is provided to control the Progress Bar.

        // idScanResultCallback.proceedToNextStep(scanResultBlob)
        // idScanResultCallback.cancel()
        // idScanResultCallback.uploadProgress(yourUploadProgressFloat)

        // LAST STEP:  On Android, the onActivityResult function in your Activity will receive control after the FaceTec SDK returns.
    }
}