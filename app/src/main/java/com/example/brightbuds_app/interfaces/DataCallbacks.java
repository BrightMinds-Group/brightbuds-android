package com.example.brightbuds_app.interfaces;

import com.example.brightbuds_app.models.ChildProfile;
import java.util.List;

public interface DataCallbacks {

    interface ChildProfileCallback {
        void onSuccess(String childId);
        void onFailure(Exception e);
    }

    interface ChildrenListCallback {
        void onSuccess(List<ChildProfile> list);
        void onFailure(Exception e);
    }

    interface GenericCallback {
        void onSuccess(String result);
        void onFailure(Exception e);
    }

    interface GenericListCallback<T> {
        void onSuccess(List<T> list);
        void onFailure(Exception e);
    }

    public interface SaveCallback {
        void onSuccess();
    }
}
