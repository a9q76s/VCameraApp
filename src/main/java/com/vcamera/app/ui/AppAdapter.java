package com.vcamera.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vcamera.app.R;
import com.vcamera.app.core.AppManager;

import java.util.ArrayList;
import java.util.List;

/**
 * محول قائمة التطبيقات
 * يعرض التطبيقات المثبتة في البيئة الافتراضية
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {
    
    private List<AppManager.AppInfo> mApps;
    private Context mContext;
    private AppClickListener mAppClickListener;
    
    public AppAdapter(Context context) {
        mContext = context;
        mApps = new ArrayList<>();
    }
    
    /**
     * تعيين قائمة التطبيقات
     */
    public void setApps(List<AppManager.AppInfo> apps) {
        mApps = apps != null ? apps : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * إضافة تطبيق إلى القائمة
     */
    public void addApp(AppManager.AppInfo app) {
        if (mApps == null) {
            mApps = new ArrayList<>();
        }
        
        mApps.add(app);
        notifyItemInserted(mApps.size() - 1);
    }
    
    /**
     * إزالة تطبيق من القائمة
     */
    public void removeApp(String packageName) {
        if (mApps == null) {
            return;
        }
        
        for (int i = 0; i < mApps.size(); i++) {
            if (mApps.get(i).getPackageName().equals(packageName)) {
                mApps.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
    
    /**
     * تعيين مستمع النقر على التطبيق
     */
    public void setAppClickListener(AppClickListener listener) {
        mAppClickListener = listener;
    }
    
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppManager.AppInfo app = mApps.get(position);
        holder.bind(app);
    }
    
    @Override
    public int getItemCount() {
        return mApps != null ? mApps.size() : 0;
    }
    
    /**
     * حامل عرض التطبيق
     */
    class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        
        private ImageView mIconView;
        private TextView mNameView;
        private TextView mPackageView;
        private AppManager.AppInfo mApp;
        
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            
            mIconView = itemView.findViewById(R.id.image_app_icon);
            mNameView = itemView.findViewById(R.id.text_app_name);
            mPackageView = itemView.findViewById(R.id.text_app_package);
            
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }
        
        /**
         * ربط بيانات التطبيق
         */
        public void bind(AppManager.AppInfo app) {
            mApp = app;
            
            // تعيين الأيقونة
            if (app.getIcon() != null) {
                mIconView.setImageDrawable(app.getIcon());
            } else {
                // تعيين أيقونة افتراضية
                mIconView.setImageResource(R.drawable.ic_app_default);
            }
            
            // تعيين الاسم
            mNameView.setText(app.getAppName());
            
            // تعيين اسم الحزمة
            mPackageView.setText(app.getPackageName());
        }
        
        @Override
        public void onClick(View v) {
            if (mAppClickListener != null && mApp != null) {
                mAppClickListener.onAppClick(mApp.getPackageName(), mApp.getUserId());
            }
        }
        
        @Override
        public boolean onLongClick(View v) {
            if (mAppClickListener != null && mApp != null) {
                mAppClickListener.onAppLongClick(mApp.getPackageName(), mApp.getUserId());
                return true;
            }
            return false;
        }
    }
    
    /**
     * واجهة للاستماع لأحداث النقر على التطبيق
     */
    public interface AppClickListener {
        void onAppClick(String packageName, int userId);
        void onAppLongClick(String packageName, int userId);
    }
}