package admin.encryption;

import android.os.Environment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Admin on 4/15/2018.
 */

class FileAdapter extends RecyclerView.Adapter {

    public static enum Status {
        SENDING , RECEIVING, OPEN, DONE, FAILED
    }

    private ArrayList fileList;
    public FileAdapter() {
        super();
        fileList = new ArrayList();
    }

    public void updateProgress(int position, long progress){
        FileModel fileModel = (FileModel) fileList.get(position);
        fileModel.received = progress;
        notifyDataSetChanged();
    }

    public void finishProgress(int position, Status status){
        FileModel fileModel = (FileModel) fileList.get(position);
        fileModel.status = status;
        notifyDataSetChanged();
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View parent = inflater.inflate(R.layout.layout_file_model,container,false);
        RecyclerView.ViewHolder holder = new FileHolder(parent);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onPopulateView(holder, fileList.get(position),position);
    }

    public void onPopulateView(RecyclerView.ViewHolder viewHolder, Object object , int position) {

        FileHolder holder = (FileHolder) viewHolder;
        FileModel fileModel = (FileModel) object;
        holder.updateWidget(fileModel);
    }

    public void addChild(Object val, int position) {
        fileList.add(position,val);
        notifyItemInserted(position);
    }

    public void addChild(Object val) {
        int position = fileList.size();
        addChild(val,position);
    }

    public void removeChild(int position) {
        fileList.remove(position);
        notifyItemRemoved(position);
    }

    public void clearAll() {
        fileList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }


    class FileHolder extends RecyclerView.ViewHolder {

        private FileModel fileModel;
        private ImageView fileImage;
        private AppCompatButton open;
        private TextView name,indicator,extension;
        public FileHolder(View itemView) {
            super(itemView);
            initializeWidget(itemView);
        }

        private void initializeWidget(View parent) {

            fileImage = parent.findViewById(R.id.file_model_image);
            open = parent.findViewById(R.id.file_model_button);
            name = parent.findViewById(R.id.file_model_name);
            indicator = parent.findViewById(R.id.file_model_progress);
            extension = parent.findViewById(R.id.file_model_extension);

            View.OnClickListener listener = param->{
                if(fileModel.status != Status.OPEN){
                    return;
                }
                File dir = new File(Environment.getExternalStorageDirectory(),"ChatMe");
                File file = new File(dir,fileModel.name);
                try {
                    Util.openFile(fileImage.getContext(), file);
                }catch (Exception e){
                    Toast.makeText(indicator.getContext(),"Error while openning file ..",Toast.LENGTH_LONG).show();
                }
            };
            open.setOnClickListener(listener);
        }

        public void updateWidget(FileModel model) {
            this.fileModel = model;
            String file = model.name;

            extension.setText(file.split("\\.")[1].toUpperCase());
            open.setText(model.status.name());
            name.setText(model.name);
            indicator.setText(model.received/1024+"/"+model.size/1024+"K");

            int [] resId = {R.drawable.icon_file_pdf, R.drawable.icon_file_doc,
                    R.drawable.icon_file_ppt, R.drawable.icon_file_xls, R.drawable.icon_file_unknown};
            if(file.endsWith(".pdf")){
                fileImage.setImageResource(resId[0]);
            }else if(file.endsWith(".doc")){
                fileImage.setImageResource(resId[1]);
            }else if(file.endsWith(".ppt")){
                fileImage.setImageResource(resId[2]);
            }else if(file.endsWith(".xls")){
                fileImage.setImageResource(resId[3]);
            }else {
                fileImage.setImageResource(resId[4]);
            }
        }

    }

}
