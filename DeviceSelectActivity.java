package admin.encryption;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        initializeWidget();

    }

    private void initializeWidget() {

        RecyclerView list = findViewById(R.id.recycler_view);
        DeviceAdapter deviceAdapter = new DeviceAdapter();
        list.setAdapter(deviceAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        list.setLayoutManager(layoutManager);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        for(BluetoothDevice device : btAdapter.getBondedDevices()){
            deviceAdapter.addChild(device);
        }
    }

    public class DeviceAdapter  extends RecyclerView.Adapter {

        private ArrayList deviceList;
        public DeviceAdapter() {
            super();
            deviceList = new ArrayList();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(container.getContext());
            View parent = inflater.inflate(R.layout.layout_device,container,false);
            RecyclerView.ViewHolder holder = new DeviceHolder(parent);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            onPopulateView(holder, deviceList.get(position),position);
        }

        public void onPopulateView(RecyclerView.ViewHolder viewHolder, Object object , int position) {

            DeviceHolder holder = (DeviceHolder) viewHolder;
            BluetoothDevice device = (BluetoothDevice) object;
            holder.updateWidget(device);
        }

        public void addChild(Object val, int position) {
            deviceList.add(position,val);
            notifyItemInserted(position);
        }

        public void addChild(Object val) {
            int position = deviceList.size();
            addChild(val,position);
        }

        public void removeChild(int position) {
            deviceList.remove(position);
            notifyItemRemoved(position);
        }

        public void clearAll() {
            deviceList.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        class DeviceHolder extends RecyclerView.ViewHolder {

            private BluetoothDevice device;
            private TextView deviceName, macAddress;

            public DeviceHolder(View itemView) {
                super(itemView);
                initializeWidget(itemView);
            }

            private void initializeWidget(View parent) {
                deviceName = parent.findViewById(R.id.name);
                macAddress = parent.findViewById(R.id.mac);

                View.OnClickListener onClickListener = param->{
                    Intent intent = new Intent(DeviceSelectActivity.this,SenderActivity.class);
                    intent.putExtra("device",device);
                    if(BluetoothConfiguration.PROTOCOL.equals("FILE_PROTOCOL")) {
                        intent.putStringArrayListExtra("files", getIntent().getStringArrayListExtra("files"));
                    }
                    startActivity(intent);
                };
                parent.setOnClickListener(onClickListener);
            }

            public void updateWidget(BluetoothDevice device) {

                this.device = device;

                String name = device.getName();
                boolean isEmpty = name == null || name.isEmpty();

                String address = device.getAddress();
                deviceName.setText(isEmpty ? "Unknown" : device.getName());
                macAddress.setText(address);
            }
        }
    }

}
