package admin.encryption;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.util.UUID;

public class ScannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
    }

    private void initializeConnection() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice device = null;
        UUID.fromString("encryption uuid");

    }


}
