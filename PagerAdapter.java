package admin.encryption;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Admin on 4/5/2018.
 */

public class PagerAdapter extends FragmentPagerAdapter {

    public PagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        return DeviceFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String [] title = {"PAIRED DEVICES","UNKNOWN DEVICES"};
        return title[position];
    }
}
