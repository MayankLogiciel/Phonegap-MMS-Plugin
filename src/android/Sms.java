package org.apache.cordova.plugin.sms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Base64;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class Sms extends CordovaPlugin {
	public final String ACTION_SEND_SMS = "send";
	private static final String INTENT_FILTER_SMS_SENT = "SMS_SENT";

	BroadcastReceiver receiver;

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		if (action.equals(ACTION_SEND_SMS)) {
			try {
				String phoneNumber = args.getJSONArray(0).join(";").replace("\"", "");
				String message = args.getString(1);
				String imageFile = args.getString(2);
				String method = args.getString(3);


				if (!checkSupport()) {
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "SMS not supported on this platform"));
					return true;
				}

				if (method.equalsIgnoreCase("INTENT")) {
					invokeSMSIntent(phoneNumber, message, imageFile);
					// always passes success back to the app
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
				} else {
					// by creating this broadcast receiver we can check whether or not the SMS was sent
					if (receiver == null) {
						this.receiver = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								PluginResult pluginResult;

								switch (getResultCode()) {
								case SmsManager.STATUS_ON_ICC_SENT:
									pluginResult = new PluginResult(PluginResult.Status.OK);
									pluginResult.setKeepCallback(true);
									callbackContext.sendPluginResult(pluginResult);
									break;
								case Activity.RESULT_OK:
									pluginResult = new PluginResult(PluginResult.Status.OK);
									pluginResult.setKeepCallback(true);
									callbackContext.sendPluginResult(pluginResult);
									break;
								case SmsManager.RESULT_ERROR_NO_SERVICE:
									pluginResult = new PluginResult(PluginResult.Status.ERROR);
									pluginResult.setKeepCallback(true);
									callbackContext.sendPluginResult(pluginResult);
									break;
								default:
									pluginResult = new PluginResult(PluginResult.Status.ERROR);
									pluginResult.setKeepCallback(true);
									callbackContext.sendPluginResult(pluginResult);
									break;
								}
							}
						};
						final IntentFilter intentFilter = new IntentFilter();
						intentFilter.addAction(INTENT_FILTER_SMS_SENT);
						cordova.getActivity().registerReceiver(this.receiver, intentFilter);
					}
					send(phoneNumber, message);
				}
				return true;
			} catch (JSONException ex) {
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
			}
		}
		return false;
	}

	private boolean checkSupport() {
		Activity ctx = this.cordova.getActivity();
		return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	@SuppressLint("NewApi")
	private void invokeSMSIntent(String phoneNumber, String message, String imageFile) {
		Intent sendIntent;
		if (!"".equals(phoneNumber)) {
			sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.putExtra("address",phoneNumber);
			sendIntent.putExtra("sms_body", message);

			// String imageDataBytes = imageFile.substring(imageFile.indexOf(",")+1);           

			// byte[] decodedString = Base64.decode(imageDataBytes, Base64.DEFAULT);
			// Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length); 

			// String saveFilePath = Environment.getExternalStorageDirectory() + "/HealthAngel";
			// File dir = new File(saveFilePath);

			// if(!dir.exists())
			// 	dir.mkdirs();

			// File file = new File(dir, "logo.png");

			// FileOutputStream fOut = null;

			// try {
			// 	fOut = new FileOutputStream(file);
			// } catch (FileNotFoundException e) {
			// 	// TODO Auto-generated catch block
			// 	e.printStackTrace();
			// }

			// decodedByte.compress(Bitmap.CompressFormat.PNG, 40, fOut);

			// try {
			// 	fOut.flush();
			// } catch (IOException e) {
			// 	// TODO Auto-generated catch block
			// 	e.printStackTrace();
			// }

			// try {
			// 	fOut.close();
			// } catch (IOException e) {
			// 	// TODO Auto-generated catch block
			// 	e.printStackTrace();
			// }

			// sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(saveFilePath + "/logo.png")));
			sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageFile));
			sendIntent.setType("image/*");
			this.cordova.getActivity().startActivity(sendIntent);
		}

	}

	private void send(String phoneNumber, String message) {
		SmsManager manager = SmsManager.getDefault();
		PendingIntent sentIntent = PendingIntent.getBroadcast(this.cordova.getActivity(), 0, new Intent(INTENT_FILTER_SMS_SENT), 0);

		// Use SendMultipartTextMessage if the message requires it
		int parts_size = manager.divideMessage(message).size();
		if (parts_size > 1) {
			ArrayList<String> parts = manager.divideMessage(message);
			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
			for (int i = 0; i < parts_size; ++i) {
				sentIntents.add(sentIntent);
			}
			manager.sendMultipartTextMessage(phoneNumber, null, parts,
					sentIntents, null);
		} else {
			manager.sendTextMessage(phoneNumber, null, message, sentIntent,
					null);
		}
	}

	@Override
	public void onDestroy() {
		if (this.receiver != null) {
			try {
				this.cordova.getActivity().unregisterReceiver(this.receiver);
				this.receiver = null;
			} catch (Exception ignore) {
			}
		}
	}
}