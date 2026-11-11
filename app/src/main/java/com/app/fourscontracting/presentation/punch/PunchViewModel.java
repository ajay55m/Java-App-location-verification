package com.app.fourscontracting.presentation.punch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.app.fourscontracting.PunchResponse;
import com.app.fourscontracting.core.network.NetworkResult;
import com.app.fourscontracting.data.repository.PunchRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PunchViewModel extends ViewModel {
    private final PunchRepository punchRepository;
    private final MutableLiveData<NetworkResult<PunchResponse>> punchResult = new MutableLiveData<>();

    public PunchViewModel() {
        this.punchRepository = new PunchRepository();
    }

    public PunchViewModel(PunchRepository punchRepository) {
        this.punchRepository = punchRepository != null ? punchRepository : new PunchRepository();
    }

    public LiveData<NetworkResult<PunchResponse>> getPunchResult() {
        return punchResult;
    }

    public void punchAttendance(int uid, int empid, String projectCode, int locationId, String type,
                                String imageBase64, double latitude, double longitude, float accuracy,
                                int isMock, String mobileSessionId) {
        punchResult.setValue(NetworkResult.loading());
        punchRepository.punchAttendance(uid, empid, projectCode, locationId, type, imageBase64,
                latitude, longitude, accuracy, isMock, mobileSessionId, new Callback<PunchResponse>() {
                    @Override
                    public void onResponse(Call<PunchResponse> call, Response<PunchResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            punchResult.setValue(NetworkResult.success(response.body()));
                        } else {
                            punchResult.setValue(NetworkResult.error("Server error: " + response.code(), null));
                        }
                    }

                    @Override
                    public void onFailure(Call<PunchResponse> call, Throwable t) {
                        punchResult.setValue(NetworkResult.error(t != null ? t.getMessage() : "Network error", null));
                    }
                });
    }
}

