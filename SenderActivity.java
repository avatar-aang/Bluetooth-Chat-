package admin.encryption;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.gigamole.library.PulseView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class SenderActivity extends AppCompatActivity {

    private BluetoothDevice bluetooth;
    private boolean shouldConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        initializeWidget();
        bluetooth = getIntent().getExtras().getParcelable("device");
        startToConnect();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initializeWidget() {
        AppCompatButton appCompatConnect = findViewById(R.id.send_button);
        View.OnClickListener onClickListener = param->{
            if(shouldConnect) {
                startToConnect();
            }
        };
        appCompatConnect.setOnClickListener(onClickListener);
    }

    private void startToConnect() {

        ConnectHandler handler = new ConnectHandler();
        Runnable runnable = new ConnectRunnable(handler);
        Thread thread = new Thread(runnable);
        thread.start();

        AppCompatButton appCompatConnect = findViewById(R.id.send_button);
        appCompatConnect.setText("Connecting ...");

        FrameLayout layout = findViewById(R.id.frame_layout);
        PulseView pulsar = findViewById(R.id.pulsar);
        layout.setVisibility(View.VISIBLE);
        pulsar.startPulse();
    }

    public class ConnectHandler extends Handler {

        @Override
        public void handleMessage(Message input) {
            super.handleMessage(input);

            FrameLayout layout = findViewById(R.id.frame_layout);
            PulseView pulsar = findViewById(R.id.pulsar);
            layout.setVisibility(View.INVISIBLE);
            pulsar.finishPulse();

            boolean isTimeOut = input.arg1 == BluetoothConfiguration.TIME_OUT_DETECT;

            AppCompatButton appCompatConnect = findViewById(R.id.send_button);
            appCompatConnect.setText("Press to reconnect");
            shouldConnect = true;

            if(isTimeOut) {
                Toast.makeText(SenderActivity.this,"Failed to connected !!!",Toast.LENGTH_LONG).show();
                return;
            }

            BluetoothSocket socket = (BluetoothSocket) input.obj;
            BluetoothDevice device = socket.getRemoteDevice();
            Toast.makeText(SenderActivity.this,"Connected to "+device.getName(),Toast.LENGTH_LONG).show();

            try {
                IOStream.IN_STREAM = new DataInputStream(socket.getInputStream());
                IOStream.OUT_STREAM = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            long size = 0;
            if(BluetoothConfiguration.PROTOCOL.equals("FILE_PROTOCOL")) {
                ArrayList files = getIntent().getStringArrayListExtra("files");
                size = files.size();
            }
            byte [] bytes = BluetoothConfiguration.PROTOCOL.getBytes();
            byte [] sizes = Util.longToBytes(size);
            try {
                IOStream.OUT_STREAM.write(bytes,0,bytes.length);
                IOStream.OUT_STREAM.write(sizes,0,sizes.length);
                IOStream.OUT_STREAM.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(BluetoothConfiguration.PROTOCOL.equals("FILE_PROTOCOL")) {
                Intent intent = new Intent(SenderActivity.this, FileSenderActivity.class);
                intent.putStringArrayListExtra("files", getIntent().getStringArrayListExtra("files"));
                startActivity(intent);
            }else {
                Intent intent = new Intent(SenderActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        }
    }


    public class ConnectRunnable implements Runnable {

        private Handler handler;

        public ConnectRunnable(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {

            BluetoothSocket socket = null;
            try {
                socket = bluetooth.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothConfiguration.UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message output = Message.obtain();
            output.obj = socket;
            output.arg1 = BluetoothConfiguration.TIME_OUT_NOT_DETECT;
            if(!socket.isConnected()){
                output.arg1 = BluetoothConfiguration.TIME_OUT_DETECT;
            }

            handler.sendMessage(output);
        }
    }

}
