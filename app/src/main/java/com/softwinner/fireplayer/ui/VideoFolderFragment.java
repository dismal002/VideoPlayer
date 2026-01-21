package com.softwinner.fireplayer.ui;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.constant.Constants;
import com.softwinner.fireplayer.provider.LocalMediaInfoAdapter;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import java.util.ArrayList;
import java.util.List;

public class VideoFolderFragment extends Fragment implements ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener {
    private static final int FOLD_MEDIA_MODE_FAVORITE = 2;
    private static final int FOLD_MEDIA_MODE_NORMAL = 0;
    private static final int FOLD_MEDIA_MODE_RECENT = 1;
    private static final String TAG = "VideoFolderFragment";
    private int mCategory = 0;
    private ExpandableListView mExpandListView;
    private ExpandableAdapter mExpandableAdapter;
    private onFolderSelectedListener mFolderSelectedListener = null;
    private List<String> mFoldersList = new ArrayList();
    /* access modifiers changed from: private */
    public LayoutInflater mLayoutInflater;
    private String[] mPreDefCategory;
    /* access modifiers changed from: private */
    public int mPreDefCategoryCount = 0;
    /* access modifiers changed from: private */
    public List<FolderInfo> mRootFoldersInfo;
    /* access modifiers changed from: private */
    public List<List<FolderInfo>> mSubFoldersInfo;

    public interface onFolderSelectedListener {
        void onFolderSelected(String str, boolean z, boolean z2, boolean z3, int i);
    }

    private class FolderInfo {
        int mFileCount;
        String mFolderName;

        public FolderInfo(String name, int filecount) {
            this.mFolderName = name;
            this.mFileCount = filecount;
        }
    }

    public void onStart() {
        super.onStart();
        loadFolders(false);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mRootFoldersInfo = new ArrayList();
        this.mSubFoldersInfo = new ArrayList();
        this.mExpandableAdapter = new ExpandableAdapter();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mLayoutInflater = inflater;
        View v = inflater.inflate(R.layout.videofolder_fragment, container, false);
        this.mExpandListView = (ExpandableListView) v.findViewById(R.id.vfolder_expand_listview);
        this.mExpandListView.setOnChildClickListener(this);
        this.mExpandListView.setOnGroupClickListener(this);
        this.mExpandListView.setAdapter(this.mExpandableAdapter);
        return v;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("SavedSelected", 1);
    }

    public void setOnFolderSelectedListener(onFolderSelectedListener listener) {
        this.mFolderSelectedListener = listener;
    }

    private int getFolderMediaCount(String folder, boolean fuzzy, int mode) {
        Cursor cursor;
        String[] videoCountProjection = {"count(_id)"};
        if (mode == 1) {
            int recorded_playtime = AppConfig.getInstance(getActivity().getApplicationContext()).getInt(AppConfig.RECORDED_PLAYTIME, 0);
            cursor = getActivity().getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, videoCountProjection, "(activevideo=1) AND (latestplaytime>?)", new String[]{String.valueOf(recorded_playtime)}, (String) null);
        } else if (mode == 2) {
            int favorite = AppConfig.getInstance(getActivity().getApplicationContext()).getInt(AppConfig.RECORDED_FAVORITE, 0);
            cursor = getActivity().getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, videoCountProjection, "(activevideo=1) AND (favorite>?)", new String[]{String.valueOf(favorite)}, (String) null);
        } else if (!fuzzy) {
            cursor = getActivity().getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, videoCountProjection, "(activevideo=1) AND (dir=?)", new String[]{folder}, (String) null);
        } else {
            cursor = getActivity().getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, videoCountProjection, "(activevideo=1) AND (dir like ?)", new String[]{"%" + folder + "%"}, (String) null);
        }
        if (cursor == null || !cursor.moveToFirst()) {
            return 0;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public void loadFolders(boolean collapseAll) {
        String mntPoint;
        List<FolderInfo> ChildFolderInfoList = null;
        this.mRootFoldersInfo.clear();
        this.mSubFoldersInfo.clear();
        this.mFoldersList.clear();
        LocalMediaInfoAdapter.getInstance(getActivity()).loadAllFolders(this.mFoldersList);
        this.mPreDefCategory = getResources().getStringArray(R.array.predef_category_array);
        this.mPreDefCategoryCount = this.mPreDefCategory.length;
        for (int i = 0; i < this.mPreDefCategoryCount; i++) {
            int videoCount = 0;
            if (i == 0) {
                videoCount = getFolderMediaCount("/", true, 0);
            } else if (i == 1) {
                videoCount = getFolderMediaCount((String) null, true, 1);
            } else if (i == 2) {
                videoCount = getFolderMediaCount("DCIM/Camera", true, 0);
            } else if (i == 3) {
                videoCount = getFolderMediaCount((String) null, true, 2);
            }
            this.mRootFoldersInfo.add(new FolderInfo(this.mPreDefCategory[i], videoCount));
            this.mSubFoldersInfo.add(new ArrayList());
        }
        String lastMntPoint = null;
        for (int i2 = 0; i2 < this.mFoldersList.size(); i2++) {
            String folder = this.mFoldersList.get(i2);
            // Find second slash after root (/storage/... or /mnt/...)
            int firstSlash = folder.indexOf("/");
            int slash_index = folder.indexOf("/", firstSlash >= 0 ? firstSlash + 1 : 0);
            if (slash_index < 0) {
                mntPoint = folder;
            } else {
                mntPoint = folder.substring(0, slash_index);
            }
            if (!mntPoint.equals(lastMntPoint)) {
                this.mRootFoldersInfo.add(new FolderInfo(mntPoint, getFolderMediaCount(mntPoint, true, 0)));
                ChildFolderInfoList = new ArrayList<>();
                this.mSubFoldersInfo.add(ChildFolderInfoList);
                lastMntPoint = mntPoint;
            }
            ChildFolderInfoList.add(new FolderInfo(folder, getFolderMediaCount(folder, false, 0)));
        }
        this.mExpandableAdapter.notifyDataSetChanged();
        if (collapseAll) {
            for (int groupPos = 0; groupPos < this.mRootFoldersInfo.size(); groupPos++) {
                this.mExpandListView.collapseGroup(groupPos);
            }
        }
    }

    private class ExpandableAdapter extends BaseExpandableListAdapter {
        public ExpandableAdapter() {
        }

        public Object getChild(int groupPosition, int childPosition) {
            return ((List) VideoFolderFragment.this.mSubFoldersInfo.get(groupPosition)).get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = VideoFolderFragment.this.mLayoutInflater.inflate(R.layout.videofolder_item_child, (ViewGroup) null);
            }
            String content = ((FolderInfo) ((List) VideoFolderFragment.this.mSubFoldersInfo.get(groupPosition)).get(childPosition)).mFolderName;
            ((TextView) view.findViewById(R.id.videofolder_item_child_name)).setText(String.valueOf(content.substring(content.lastIndexOf("/") + 1)) + " (" + ((FolderInfo) ((List) VideoFolderFragment.this.mSubFoldersInfo.get(groupPosition)).get(childPosition)).mFileCount + ")");
            return view;
        }

        public int getChildrenCount(int groupPosition) {
            return ((List) VideoFolderFragment.this.mSubFoldersInfo.get(groupPosition)).size();
        }

        public Object getGroup(int groupPosition) {
            return VideoFolderFragment.this.mRootFoldersInfo.get(groupPosition);
        }

        public int getGroupCount() {
            return VideoFolderFragment.this.mRootFoldersInfo.size();
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = VideoFolderFragment.this.mLayoutInflater.inflate(R.layout.videofolder_item_group, (ViewGroup) null);
            }
            ImageView iv = (ImageView) view.findViewById(R.id.videofolder_item_expand);
            ImageView group_icon = (ImageView) view.findViewById(R.id.videofolder_item_group_icon);
            if (groupPosition >= VideoFolderFragment.this.mPreDefCategoryCount) {
                if (!isExpanded) {
                    iv.setBackgroundResource(R.drawable.expander_ic_minimized);
                } else {
                    iv.setBackgroundResource(R.drawable.expander_ic_maximized);
                }
                group_icon.setBackgroundResource(R.drawable.ic_storage);
            } else {
                iv.setBackgroundDrawable((Drawable) null);
                if (groupPosition == 0) {
                    group_icon.setBackgroundResource(R.drawable.ic_all_viedos);
                } else if (groupPosition == 1) {
                    group_icon.setBackgroundResource(R.drawable.ic_recent_play);
                } else if (groupPosition == 2) {
                    group_icon.setBackgroundResource(R.drawable.ic_camcorder);
                } else if (groupPosition == 3) {
                    group_icon.setBackgroundResource(R.drawable.ic_favorite2);
                }
            }
            ((TextView) view.findViewById(R.id.videofolder_item_group_name)).setText(String.valueOf(((FolderInfo) VideoFolderFragment.this.mRootFoldersInfo.get(groupPosition)).mFolderName) + " (" + ((FolderInfo) VideoFolderFragment.this.mRootFoldersInfo.get(groupPosition)).mFileCount + ")");
            return view;
        }

        public boolean hasStableIds() {
            return false;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        parent.setItemChecked(parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition)), true);
        this.mFolderSelectedListener.onFolderSelected(((FolderInfo) this.mSubFoldersInfo.get(groupPosition).get(childPosition)).mFolderName, false, true, false, childPosition);
        return false;
    }

    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        parent.setItemChecked(parent.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(groupPosition)), true);
        if (groupPosition < this.mPreDefCategoryCount) {
            this.mFolderSelectedListener.onFolderSelected(Constants.PREDEF_CATEGORY[groupPosition], false, true, true, groupPosition);
            return false;
        }
        this.mFolderSelectedListener.onFolderSelected(this.mRootFoldersInfo.get(groupPosition).mFolderName, true, false, true, groupPosition);
        return false;
    }

    public int getCategory() {
        return this.mCategory;
    }

    public void setCategory(int category) {
        this.mCategory = category;
        if (this.mExpandListView == null) {
            return;
        }
        if (this.mCategory == 0) {
            this.mExpandListView.setItemChecked(0, true);
        } else if (this.mCategory == 5) {
            this.mExpandListView.setItemChecked(this.mExpandListView.getCheckedItemPosition(), false);
        }
    }
}
