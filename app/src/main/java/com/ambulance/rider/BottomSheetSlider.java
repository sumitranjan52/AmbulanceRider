package com.ambulance.rider;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by sumit on 24-Jan-18.
 */

public class BottomSheetSlider extends BottomSheetDialogFragment {

    String mPickup, mDestination;

    public static BottomSheetSlider newInstance(String pickup, String destination) {
        BottomSheetSlider fragment = new BottomSheetSlider();
        Bundle args = new Bundle();
        args.getString("pickup", pickup);
        args.getString("destination", destination);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPickup = getArguments().getString("pickup");
        mDestination = getArguments().getString("destination");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_rider, container, false);
        TextView pickup = view.findViewById(R.id.pickupAddress);
        TextView drop = view.findViewById(R.id.dropAddress);
        TextView fare = view.findViewById(R.id.fare);
        pickup.setText("Pickup: " + mPickup);
        drop.setText("Destination: " + mDestination);
        fare.setText("Estimated fare: 10Km + 15min = ₹100");
        return view;
    }
}
