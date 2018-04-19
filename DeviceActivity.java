package admin.encryption;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class DeviceActivity extends AppCompatActivity {


    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        initialize();
        initializeWidget();
        registerCallback();
    }

    @Override
    public void onBackPressed() {
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }else {
            super.onBackPressed();
        }
    }

    private void registerCallback(){
        IntentReceiver receiver = new IntentReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
    }

    private void initialize(){

        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            Toast.makeText(this,"device does not supports !!!",Toast.LENGTH_LONG).show();
        }
    }

    private void initializeWidget() {


        TextView deviceTitle = findViewById(R.id.device_name);
        TextView deviceMac = findViewById(R.id.device_mac);

        deviceTitle.setText(btAdapter.getName());
        deviceMac.setText(btAdapter.getAddress());

        SwitchCompat switchCompat = findViewById(R.id.switcher);
        switchCompat.setChecked(isBluetoothEnabled());
        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (button, checked) -> {

            boolean enabled = isBluetoothEnabled();

            if(checked == true && enabled == false) {
                setEnable(true);
            }else if(checked == false && enabled == true){
                setEnable(false);
            }
        };
        switchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
        //switchCompat.setOnClickListener(param -> startActivity(new Intent(this,ScannerActivity.class)));

        FragmentPagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.view_pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(1);
        TabLayout layout = findViewById(R.id.tab_layout);
        layout.setupWithViewPager(pager);

        View.OnClickListener sendClickListener = param->{
            if(!isBluetoothEnabled()){
                Toast.makeText(this,"Enable Bluetooth First !!!",Toast.LENGTH_LONG).show();
                return;
            }

            PopupMenu.OnMenuItemClickListener onMenuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()){
                        case R.id.menu_chat:
                            BluetoothConfiguration.PROTOCOL = "CHAT_PROTOCOL";
                            Intent intent = new Intent(DeviceActivity.this,DeviceSelectActivity.class);
                            startActivity(intent);
                            break;
                        case R.id.menu_file:
                            BluetoothConfiguration.PROTOCOL = "FILE_PROTOCOL";
                            FilePickerBuilder.getInstance().setMaxCount(5).pickFile(DeviceActivity.this);
                            break;
                        default:
                    }
                    return false;
                }
            };
            PopupMenu popupMenu = new PopupMenu(DeviceActivity.this,pager);
            Menu menu = popupMenu.getMenu();
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.menu_protocol,menu);
            popupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            popupMenu.show();

            //Intent intent = new Intent(this,DeviceSelectActivity.class);
            //startActivity(intent);
        };
        AppCompatButton appCompatSend = findViewById(R.id.send_button);
        appCompatSend.setOnClickListener(sendClickListener);
        View.OnClickListener receiveClickListener = param->{

            if(!isBluetoothEnabled()){
                Toast.makeText(this,"Enable Bluetooth First !!!",Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(this,ReceiverActivity.class);
            startActivity(intent);
        };
        AppCompatButton appCompatReceive = findViewById(R.id.receive_button);
        appCompatReceive.setOnClickListener(receiveClickListener);

    }

    private void setEnable(boolean enable) {

        if(enable == true){
            btAdapter.enable();
        }else {
            btAdapter.disable();
        }
    }

    private boolean isBluetoothEnabled() {

        return btAdapter.isEnabled();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case FilePickerConst.REQUEST_CODE_DOC:
                if(resultCode== Activity.RESULT_OK && data!=null) {
                    ArrayList paths = new ArrayList<>();
                    paths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));

                    Intent intent = new Intent(this,DeviceSelectActivity.class);
                    intent.putStringArrayListExtra("files",paths);
                    startActivity(intent);
                }
                break;
        }
    }

    class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            ViewPager pager = findViewById(R.id.view_pager);
            View parent = pager.getChildAt(1);

            String message = null;
            String action = intent.getAction();

            switch (action){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    message = "DISCOVERY STARTED";

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    message = "PAIRED DEVICE FOUND";

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    RecyclerView list = parent.findViewById(R.id.recycler_view);
                    DeviceAdapter deviceAdapter = (DeviceAdapter) list.getAdapter();
                    if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                        deviceAdapter.addChild(device);
                        message = "NEW DEVICE FOUND";
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    message = "DISCOVERY FINISHED";

                    SwipeRefreshLayout layout = parent.findViewById(R.id.swipe_refresh_layout);
                    if(layout.isRefreshing()){
                        layout.setRefreshing(false);
                    }
                    break;
                default:
            }
            Snackbar.make(pager,message,Snackbar.LENGTH_LONG).show();
        }
    }
}
