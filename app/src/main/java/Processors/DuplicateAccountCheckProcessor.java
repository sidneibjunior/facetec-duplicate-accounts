//
// Welcome to the annotated FaceTec Device SDK core code for performing secure Liveness Checks!
//
package Processors;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facetec.sampleapp.SampleAppActivity;
import com.facetec.sdk.FaceTecCustomization;
import com.facetec.sdk.FaceTecFaceScanProcessor;
import com.facetec.sdk.FaceTecFaceScanResultCallback;
import com.facetec.sdk.FaceTecSDK;
import com.facetec.sdk.FaceTecSessionActivity;
import com.facetec.sdk.FaceTecSessionResult;
import com.facetec.sdk.FaceTecSessionStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;

// This is an example self-contained class to perform Liveness Checks with the FaceTec SDK.
// You may choose to further componentize parts of this in your own Apps based on your specific requirements.

// Android Note 1:  Some commented "Parts" below are out of order so that they can match iOS and Browser source for this same file on those platforms.
// Android Note 2:  Android does not have a onFaceTecSDKCompletelyDone function that you must implement like "Part 10" of iOS and Android Samples.  Instead, onActivityResult is used as the place in code you get control back from the FaceTec SDK.
public class DuplicateAccountCheckProcessor extends Processor implements FaceTecFaceScanProcessor {
    private boolean success = false;
    final private SampleAppActivity sampleAppActivity;
    private String userId;

    public DuplicateAccountCheckProcessor(String sessionToken, Context context, String userId) {
        this.sampleAppActivity = (SampleAppActivity) context;
        this.userId = userId;

        //
        // Part 1:  Starting the FaceTec Session
        //
        // Required parameters:
        // - Context:  Unique for Android, a Context is passed in, which is required for the final onActivityResult function after the FaceTec SDK is done.
        // - FaceTecFaceScanProcessor:  A class that implements FaceTecFaceScanProcessor, which handles the FaceScan when the User completes a Session.  In this example, "self" implements the class.
        // - sessionToken:  A valid Session Token you just created by calling your API to get a Session Token from the Server SDK.
        //
        FaceTecSessionActivity.createAndLaunchSession(context, DuplicateAccountCheckProcessor.this, sessionToken);
    }

    //
    // Part 2:  Handling the Result of a FaceScan
    //
    public void processSessionWhileFaceTecSDKWaits(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback) {
        //
        // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
        // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
        //
        sampleAppActivity.setLatestSessionResult(sessionResult);

        //
        // Part 3:  Handles early exit scenarios where there is no FaceScan to handle -- i.e. User Cancellation, Timeouts, etc.
        //
        if(sessionResult.getStatus() != FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY) {
            NetworkingHelpers.cancelPendingRequests();
            faceScanResultCallback.cancel();
            return;
        }

        //
        // Part 4:  Get essential data off the FaceTecSessionResult
        //
        JSONObject parameters = new JSONObject();
        try {

            Log.i("facetec faceScan", sessionResult.getFaceScanBase64());
            Log.i("facetec auditTrailImage", sessionResult.getAuditTrailCompressedBase64()[0]);
            Log.i("facetec low", sessionResult.getLowQualityAuditTrailCompressedBase64()[0]);
            parameters.put("faceScan", sessionResult.getFaceScanBase64());
            parameters.put("auditTrailImage", sessionResult.getAuditTrailCompressedBase64()[0]);
            parameters.put("lowQualityAuditTrailImage", sessionResult.getLowQualityAuditTrailCompressedBase64()[0]);
            parameters.put("externalDatabaseRefID", getExternalDatabaseRefID());
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        //
        // Part 5:  Make the Networking Call to Your Servers.  Below is just example code, you are free to customize based on how your own API works.
        //
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Config.BaseURL + "/enrollment-3d")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", Config.DeviceKeyIdentifier)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(sessionResult.getSessionId()))

                //
                // Part 7:  Demonstrates updating the Progress Bar based on the progress event.
                //
                .post(new ProgressRequestBody(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString()),
                        new ProgressRequestBody.Listener() {
                            @Override
                            public void onUploadProgressChanged(long bytesWritten, long totalBytes) {
                                final float uploadProgressPercent = ((float)bytesWritten) / ((float)totalBytes);
                                faceScanResultCallback.uploadProgress(uploadProgressPercent);
                            }
                        }))
                .build();

        //
        // Part 8:  Actually send the request.
        //
        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {

                //
                // Part 6:  In our Sample, we evaluate a boolean response and treat true as was successfully processed and should proceed to next step,
                // and handle all other responses by cancelling out.
                // You may have different paradigms in your own API and are free to customize based on these.
                //

                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    boolean error = responseJSON.getBoolean("error");
                    final boolean success = responseJSON.getBoolean("success");
                    if (error) {
                        String errorMessage = responseJSON.getString("errorMessage");
                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "Error: " + errorMessage, Toast.LENGTH_LONG).show());
                        faceScanResultCallback.cancel();
                        return;
                    }
                    boolean wasProcessed = responseJSON.getBoolean("wasProcessed");
                    String scanResultBlob = responseJSON.getString("scanResultBlob");
                    String faceScanSecurityChecks = responseJSON.getString("faceScanSecurityChecks");

                    // In v9.2.0+, we key off a new property called wasProcessed to determine if we successfully processed the Session result on the Server.
                    // Device SDK UI flow is now driven by the proceedToNextStep function, which should receive the scanResultBlob from the Server SDK response.
                    if (wasProcessed) {

                        // Demonstrates dynamically setting the Success Screen Message.
                        FaceTecCustomization.overrideResultScreenSuccessMessage = "Liveness\nConfirmed";

                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "Success: " + success + "\n\n" + faceScanSecurityChecks, Toast.LENGTH_LONG).show());

                        run3dDBEnroll(sessionResult, faceScanResultCallback, scanResultBlob);

                    } else {
                        // CASE:  UNEXPECTED response from API.  Our Sample Code keys off a wasProcessed boolean on the root of the JSON object --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                        faceScanResultCallback.cancel();
                    }
                }
                catch(JSONException e) {
                    // CASE:  Parsing the response into JSON failed --> You define your own API contracts with yourself and may choose to do something different here based on the error.  Solid server-side code should ensure you don't get to this case.
                    e.printStackTrace();
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    faceScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @Nullable IOException e) {
                // CASE:  Network Request itself is erroring --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                faceScanResultCallback.cancel();
            }
        });
    }

    private void run3dDBEnroll(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback, final String scanResultBlob) {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("externalDatabaseRefID", getExternalDatabaseRefID());
            parameters.put("groupName", "mode_mobile_test");
        } catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Config.BaseURL + "/3d-db/enroll")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", Config.DeviceKeyIdentifier)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(sessionResult.getSessionId()))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString())).build();

        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    final boolean success = responseJSON.getBoolean("success");
                    if (!success) {
                        String errorMessage = responseJSON.getString("errorMessage");
                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "Error 3d-db/enroll: " + errorMessage, Toast.LENGTH_LONG).show());
                        faceScanResultCallback.cancel();
                        return;
                    }

                    run3dDbSearch(sessionResult, faceScanResultCallback, scanResultBlob);

                }
                catch(JSONException e) {
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    faceScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @Nullable IOException e) {
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                faceScanResultCallback.cancel();
            }
        });
    }

    private void run3dDbSearch(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback, final String scanResultBlob) {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("externalDatabaseRefID", getExternalDatabaseRefID());
            parameters.put("groupName", "mode_mobile_test");
        } catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Config.BaseURL + "/3d-db/search")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", Config.DeviceKeyIdentifier)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(sessionResult.getSessionId()))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString())).build();

        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    final boolean reqSuccess = responseJSON.getBoolean("success");

                    if (!reqSuccess) {
                        String errorMessage = responseJSON.getString("errorMessage");
                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "Error 3d-db/search: " + errorMessage, Toast.LENGTH_LONG).show());
                        faceScanResultCallback.cancel();
                        return;
                    }

                    final JSONArray results = responseJSON.getJSONArray("results");
                    if (results.length() > 1) {
                        String ids = "";
                        String comma = "";
                        for(int i = 0; i < results.length(); i++){
                            JSONObject o = results.getJSONObject(i);
                            ids += comma + o.getString("identifier");
                            comma = ", ";
                        }
                        final String duplicateAccountIds = ids;
                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "Duplicate accounts found: " + (results.length()) + "\n" + duplicateAccountIds, Toast.LENGTH_LONG).show());
                    } else {
                        sampleAppActivity.runOnUiThread(() -> Toast.makeText(sampleAppActivity, "No duplicate accounts found!", Toast.LENGTH_LONG).show());
                    }

                    // In v9.2.0+, simply pass in scanResultBlob to the proceedToNextStep function to advance the User flow.
                    // scanResultBlob is a proprietary, encrypted blob that controls the logic for what happens next for the User.
                    success = faceScanResultCallback.proceedToNextStep(scanResultBlob);

                }
                catch(JSONException e) {
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    faceScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @Nullable IOException e) {
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                faceScanResultCallback.cancel();
            }
        });

    }

    private String getExternalDatabaseRefID() {
        return "mode_mobile_" + userId;
    }

    public boolean isSuccess() {
        return this.success;
    }
}