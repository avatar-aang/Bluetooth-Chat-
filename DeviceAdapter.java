package admin.encryption;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Admin on 4/5/2018.
 */

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
                int state = device.getBondState();
                if(state != BluetoothDevice.BOND_BONDED){
                    device.createBond();
                }
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
