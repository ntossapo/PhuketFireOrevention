package com.example.ntossapo.phuketprevention.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.example.ntossapo.phuketprevention.AppStatus;
import com.example.ntossapo.phuketprevention.Command;
import com.example.ntossapo.phuketprevention.R;
import com.example.ntossapo.phuketprevention.adapter.EventAdapter;

import java.util.ArrayList;

/**
 * Created by Tossapon on 26/12/2557.
 */
/**
 * A accidentList fragment containing a simple view.
 */
public class EventListFragment extends Fragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    public static EventListFragment newInstance() {
        EventListFragment fragment = new EventListFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public EventListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_accidentlist, container, false);
        ListView list = (ListView) rootView.findViewById(R.id.list);
        if(AppStatus.MODE.equals("offline") && AppStatus.DATA.getAccidentData().size() == 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, new String[]{"คุณทำงานบนออฟไลน์โหมด"});
            list.setAdapter(adapter);
        }else if(AppStatus.MODE.equals("offline") && AppStatus.DATA.getAccidentData().size() != 0){
            EventAdapter ea = new EventAdapter(AppStatus.DATA.getAccidentData(), getActivity());
            list.setAdapter(ea);
        }else if(AppStatus.MODE.equals("online")){
            if(Command.NeedUpdate(getActivity()))
                Command.SyncEvent(getActivity());
            EventAdapter ea = new EventAdapter(AppStatus.DATA.getAccidentData(), getActivity());
            list.setAdapter(ea);
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}

