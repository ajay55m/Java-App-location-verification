package com.app.fourscontracting.ui.attendance;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.app.fourscontracting.data.AttendancePayload;
import com.app.fourscontracting.data.AttendanceRepository;
import com.app.fourscontracting.data.AttendanceResult;

/**
 * ViewModel for attendance submit / offline flush (MVVM entry for LocationActivity).
 */
public class AttendanceViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private final MutableLiveData<AttendanceResult> lastResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);

    public AttendanceViewModel(@NonNull Application application) {
        super(application);
        repository = new AttendanceRepository(application);
    }

    public LiveData<AttendanceResult> getLastResult() {
        return lastResult;
    }

    public LiveData<Boolean> isSubmitting() {
        return submitting;
    }

    public int pendingOfflineCount() {
        return repository.pendingCount();
    }

    public void submit(AttendancePayload payload) {
        submitting.setValue(true);
        repository.submit(payload, result -> {
            submitting.postValue(false);
            lastResult.postValue(result);
        });
    }

    public void flushOfflineQueue() {
        repository.flushQueue(result -> {
            if (result != null && result.success && !result.queuedOffline) {
                lastResult.postValue(result);
            }
        });
    }
}
