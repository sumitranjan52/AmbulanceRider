package com.ambulance.rider.Common;

import android.location.Location;

import com.ambulance.rider.Remote.FCMClient;
import com.ambulance.rider.Remote.IFCMService;

/**
 * Created by sumit on 26-Jan-18.
 */

public class Common {

    public static final String driverInfo = "DriverInformation";
    public static final String riderInfo = "RiderInformation";
    public static final String driverLoc = "DriverLocation";
    public static final String requestRide = "RequestRide";
    public static final String tokens = "MessagingTokens";

    public static Location mLastLocation = null;

    public static final String fcmURL = "https://fcm.googleapis.com/";

    public static IFCMService getFCMService(){
        return FCMClient.getRetrofit(fcmURL).create(IFCMService.class);
    }

}
