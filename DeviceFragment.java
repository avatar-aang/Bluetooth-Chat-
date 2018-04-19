package admin.encryption;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by Admin on 4/1/2018.
 */

public class DeviceFragment extends Fragment {

    public static DeviceFragment newInstance(int section) {

        Bundle bundle = new Bundle();
        bundle.putInt("section",section);
        DeviceFragment deviceFragment = new DeviceFragment();
        deviceFragment.setArguments(bundle);

        return deviceFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        int section = getArguments().getInt("section");
        View parent = inflater.inflate(R.layout.layout_fragment,container,false);

        DeviceAdapter deviceAdapter = new DeviceAdapter();
        RecyclerView list = parent.findViewById(R.id.recycler_view);
        list.setAdapter(deviceAdapter);
        LinearLayoutManager pairLayoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        list.setLayoutManager(pairLayoutManager);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        SwipeRefreshLayout layout = parent.findViewById(R.id.swipe_refresh_layout);
        layout.setColorSchemeResources(R.color.colorRed,R.color.colorGreen,R.color.colorBlue);
        SwipeRefreshLayout.OnRefreshListener onRefreshListener = ()->{

            if(!btAdapter.isEnabled()){
                Toast.makeText(getContext(),"Enable Bluetooth First !",Toast.LENGTH_LONG).show();
                layout.setRefreshing(false);
                return;
            }

            if(section == 0){
                deviceAdapter.clearAll();
                for(BluetoothDevice device : btAdapter.getBondedDevices()){
                    deviceAdapter.addChild(device);
                }
                if(layout.isRefreshing()){
                    layout.setRefreshing(false);
                }
            }else {
                if(!btAdapter.isDiscovering()){
                    deviceAdapter.clearAll();
                    btAdapter.startDiscovery();
                }else {
                    Toast.makeText(getContext(),"Already Discovering !",Toast.LENGTH_LONG).show();
                }
            }
        };
        layout.setOnRefreshListener(onRefreshListener);
        return parent;
    }
}
