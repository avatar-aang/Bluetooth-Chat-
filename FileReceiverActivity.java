package admin.encryption;

import android.bluetooth.BluetoothDevice;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileReceiverActivity extends AppCompatActivity {

    private static final int ACTION_ADD = 0x02;
    private static final int ACTION_UPDATE = 0x04;
    private static final int ACTION_FINISH = 0x08;
    private static final int ACTION_ERROR = 0x10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_receiver);

        initializeWidget();
        initialize();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            IOStream.IN_STREAM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            IOStream.OUT_STREAM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initializeWidget() {

        FileAdapter fileAdapter = new FileAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);

        RecyclerView list = findViewById(R.id.recycler_view);
        list.setAdapter(fileAdapter);
        list.setLayoutManager(layoutManager);
    }

    private void initialize() {

        Handler handler = new FileHandler();
        FileRunnable runnable = new FileRunnable(handler);
        Thread thread = new Thread(runnable);
        thread.start();
    }


    class FileHandler extends Handler {
        @Override
        public void handleMessage(Message input) {
            super.handleMessage(input);
            RecyclerView list = findViewById(R.id.recycler_view);
            FileAdapter fileAdapter = (FileAdapter) list.getAdapter();
            switch (input.arg1){
                case ACTION_ADD:
                    fileAdapter.addChild(input.obj);
                    //fileAdapter.finishProgress(input.arg2, FileAdapter.Status.RECEIVING);
                    break;
                case ACTION_UPDATE:
                    fileAdapter.updateProgress(input.arg2, (Long) input.obj);
                    break;
                case ACTION_FINISH:
                    fileAdapter.finishProgress(input.arg2, FileAdapter.Status.OPEN);
                    break;
                case ACTION_ERROR:
                    fileAdapter.finishProgress(input.arg2, FileAdapter.Status.FAILED);
                    break;
                default:
            }
        }
    }

    class FileRunnable implements Runnable {

        private Handler handler;
        public FileRunnable(Handler handler){
            this.handler = handler;
        }
        @Override
        public void run() {

            DataInputStream inputStream = IOStream.IN_STREAM;
            long count = BluetoothConfiguration.FILE_SIZE;
            Log.d("spice-fr-size",count+"");
            int loop = 0;
            while (loop < count) {

                Message output = null;
                output = Message.obtain();

                byte [] bytes = new byte[64];
                try {
                    inputStream.read(bytes,0,32);
                } catch (IOException e) {
                    e.printStackTrace();

                    output = Message.obtain();
                    output.arg1 = ACTION_ERROR;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                    return;
                }
                String file = new String(bytes,0,32);
                file = file.trim();
                try {
                    inputStream.read(bytes,0,8);
                } catch (IOException e) {
                    e.printStackTrace();

                    output = Message.obtain();
                    output.arg1 = ACTION_ERROR;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                    return;
                }

                long size = Util.bytesToLong(bytes);
                Log.d("spice--fileSize",size+"");
                output = Message.obtain();
                output.obj = new FileModel(file,0,size, FileAdapter.Status.RECEIVING);
                output.arg1 = ACTION_ADD;

                handler.sendMessage(output);

                File dir = new File(Environment.getExternalStorageDirectory(),"ChatMe");
                if(!dir.exists()){
                    dir.mkdir();
                }
                File storage = new File(dir,file);
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(storage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                long received = 0;
                long receiveCount = size / 64;

                for(int index = 0 ; index < receiveCount ; index++){
                    try {
                        inputStream.read(bytes,0,64);
                        outputStream.write(bytes,0,64);
                        outputStream.flush();
                        received += 64;

                        output = Message.obtain();
                        output.obj = received;
                        output.arg1 = ACTION_UPDATE;
                        output.arg2 = loop;
                        handler.sendMessage(output);

                    } catch (IOException e) {
                        e.printStackTrace();
                        output = Message.obtain();
                        output.arg1 = ACTION_ERROR;
                        output.arg2 = loop;
                        handler.sendMessage(output);
                        return;
                    }
                }
                int remain = (int) (size % 64);
                try {
                    inputStream.read(bytes,0,remain);
                    outputStream.write(bytes,0,remain);
                    outputStream.flush();
                    received += remain;

                    output = Message.obtain();
                    output.obj = received;
                    output.arg1 = ACTION_UPDATE;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                } catch (IOException e) {
                    e.printStackTrace();
                    output = Message.obtain();
                    output.arg1 = ACTION_ERROR;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                    return;
                }

                output = Message.obtain();
                output.arg1 = ACTION_FINISH;
                output.arg2 = loop;
                handler.sendMessage(output);

                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                loop++;
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
